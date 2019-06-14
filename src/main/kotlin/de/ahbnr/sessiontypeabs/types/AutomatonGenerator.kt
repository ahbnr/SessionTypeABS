package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.SessionAutomaton
import de.ahbnr.sessiontypeabs.Transition
import de.ahbnr.sessiontypeabs.TransitionVerb

fun genAutomaton(t: LocalType) = genAutomaton(t, Cache())

/**
 * Helper class for the automaton generator functions in this file.
 *
 * It keeps track of already used state and register names.
 * It also tracks which future is supposed to be stored in which register and to which methods a future name is linked.
 */
private data class Cache(
  var nextFreeState: Int = 0,
  var nextFreeRegister: Int = 0,
  var assignedRegisters: MutableMap<Future, Int> = mutableMapOf(),
  var futureToMethod: MutableMap<Future, Method> = mutableMapOf()
) {
  /**
   * Determines the name of the register in which a future of name [f] is supposed to be stored or allocates a new
   * register name otherwise
   */
  fun registerForFuture(f: Future) =
    assignedRegisters.getOrElse(f) {
      val reg = nextFreeRegister++
      assignedRegisters.plusAssign(Pair(f, reg))

      return reg
    }

  /**
   * Allocates the next unused name of a state
   */
  fun nextState() = nextFreeState++

  /**
   * Retrieves the name of the method upon whose invocation [f] was created.
   */
  fun findMethodForFuture(f: Future) = futureToMethod[f]

  /**
   * Stores the name [m] of the method of which the invocation created the future [f].
   *
   * Should be called whenever invocREv is encountered while building an automaton.
   */
  fun saveMethodForFuture(f: Future, m: Method) =
    futureToMethod.plusAssign(Pair(f, m))
}

/**
 * Concatenates automata at given "glue" states.
 *
 * The resulting automaton will not contain the initial states of the second automaton.
 * Instead the initial transitions of the second automaton will start with the [glueStates].
 *
 * @param a0 first automaton
 * @param glueStates in the resulting automaton, copies all initial transitions of [a1] will be included for each glue
 *                   state as its starting state
 * @param a1 second automaton
 * @param finalStates states which shall act as final states of the resulting automaton
 * @return concatenation of a0 and a1 at the glue states
 * @throws IllegalArgumentException if the glue states are not part of [a1]
 */
private fun concatAutomata(a0: SessionAutomaton, glueStates: Set<Int>, a1: SessionAutomaton, finalStates: Set<Int>): SessionAutomaton {
  if (!a0.Q.containsAll(glueStates)) {
    throw IllegalArgumentException("To concatenate automata the glue states must all be part of the first automaton")
  }

  val a1InitialTransitions = a1.transitionsForState(a1.q0)
  val replacementTransitions = a1InitialTransitions.map{
    initialT -> glueStates.map{qG -> Transition(qG, initialT.verb, initialT.q2)}
  }.flatten()

  return SessionAutomaton(
    a0.Q.union(a1.Q.minus(a1.q0)), // Q1 ∪ Q2 \ {a1.q0}
    a0.q0,
    a0.Delta // Δ1 ∪ (Δ2 \ {(a1.q0, <...>)}) ∪ {(q, <...>) | q ∈ F1}
      .union(a1.Delta.minus(a1InitialTransitions))
      .union(replacementTransitions),
    a0.registers.union(a1.registers),
    finalStates
  )
}

private fun genAutomaton(t: LocalType.InvocationRecv, c: Cache): SessionAutomaton {
  val q0 = c.nextState()
  val q1 = c.nextState()
  val r0 = c.registerForFuture(t.f)

  c.saveMethodForFuture(t.f, t.m)

  return SessionAutomaton(
    setOf(q0, q1),
    q0,
    setOf(
      Transition(q0, TransitionVerb.InvocREv(t.m, r0), q1)
    ),
    setOf(r0),
    setOf(q1)
  )
}

private fun genAutomaton(t: LocalType.Reactivation, c: Cache): SessionAutomaton {
  val q0 = c.nextState()
  val q1 = c.nextState()
  val r0 = c.registerForFuture(t.f)
  val method =
    c.findMethodForFuture(t.f)
      ?:throw IllegalArgumentException("Reactivation of a future which has not been created yet by an invocation.")

  return SessionAutomaton(
    setOf(q0, q1),
    q0,
    setOf(
      Transition(q0, TransitionVerb.ReactEv(method, r0), q1)
    ),
    setOf(r0),
    setOf(q1)
  )
}

private fun genAutomaton(t: LocalType.Concatenation, c: Cache): SessionAutomaton {
  val a0 = genAutomaton(t.lhs, c)
  val a1 = genAutomaton(t.rhs, c)

  return concatAutomata(
    a0,
    a0.finalStates,
    a1,
    a1.finalStates
  )
}

private fun genAutomaton(t: LocalType.Repetition, c: Cache): SessionAutomaton {
  val a = genAutomaton(t.repeatedType, c)

  val initialTransitions = a.transitionsForState(a.q0)
  val additionalBackTransitions =
    initialTransitions.map{
      initialT -> a.finalStates.map{qF -> Transition(qF, initialT.verb, initialT.q2)}
    }.flatten()

  return SessionAutomaton(
    a.Q,
    a.q0,
    a.Delta // Δ ∪ {(q, v, q') | q ∈ F1 ∧ (q0, v, q') ∈ ∆}
      .union(additionalBackTransitions),
    a.registers,
    a.finalStates.union(listOf(a.q0))
  )
}

private fun genAutomaton(t: LocalType.Branching, c: Cache): SessionAutomaton {
  // Return a single state automaton if there are no types to choose from in this branching
  if (t.choices.isEmpty()) {
    val q0 = c.nextState()

    return SessionAutomaton(
      setOf(q0),
      q0,
      emptySet(),
      emptySet(),
      setOf(q0)
    )
  }

  else {
    val head = t.choices.first()
    val tail = t.choices.drop(1)

    // Construct an automaton for the first branch and recurse on the other ones.
    val headAutomaton = genAutomaton(head, c)
    val tailAutomaton = genAutomaton(
      LocalType.Branching(tail),
      c
    )

    // TODO: Enforce determinism
    // Either here or by providing a generic transformation function to generate
    // deterministic Session Automata from non-deterministic ones.
    // Or dont allow non deterministic types or transform types into
    // deterministic ones.

    // Join the automaton of the first branch and the one constructed from the rest by gluing them together on their
    // initial state:
    return concatAutomata(
      headAutomaton,
      setOf(headAutomaton.q0),
      tailAutomaton,
      headAutomaton.finalStates union
        if (t.choices.size == 1) { // Reason for If see above
          emptySet()
        }
        else {
          headAutomaton.finalStates
        }
    )
  }
}

private fun genAutomaton(t: LocalType, c: Cache) =
  when (t) { // TODO use visitor pattern instead?
    is LocalType.Branching      -> genAutomaton(t, c)
    is LocalType.Concatenation  -> genAutomaton(t, c)
    is LocalType.Reactivation   -> genAutomaton(t, c)
    is LocalType.Repetition     -> genAutomaton(t, c)
    is LocalType.InvocationRecv -> genAutomaton(t, c)
  }


