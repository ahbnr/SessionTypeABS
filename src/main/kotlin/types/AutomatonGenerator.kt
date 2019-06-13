package de.ahbnr.sessiontypeabs.types;

import de.ahbnr.sessiontypeabs.SessionAutomaton;
import de.ahbnr.sessiontypeabs.Transition;
import de.ahbnr.sessiontypeabs.TransitionVerb;

data class Cache(
  var nextFreeState: Int = 0,
  var nextFreeRegister: Int = 0,
  var assignedRegisters: MutableMap<Future, Int> = mutableMapOf(),
  var futureToMethod: MutableMap<Future, Method> = mutableMapOf()
) {
  fun registerForFuture(f: Future) =
    assignedRegisters.getOrElse(f) {
      val reg = nextFreeRegister++;
      assignedRegisters.plusAssign(Pair(f, reg));

      return reg;
    }

  fun nextState() = nextFreeState++

  fun findMethodForFuture(f: Future) = futureToMethod.get(f)
  fun saveMethodForFuture(f: Future, m: Method) =
    futureToMethod.plusAssign(Pair(f, m))
}

fun concatAutomata(a0: SessionAutomaton, glueStates: Set<Int>, a1: SessionAutomaton, finalStates: Set<Int>): SessionAutomaton {
  // TODO: Throw exception if glue states are not part of a0

  val a1InitialTransitions = a1.transitionsForState(a1.q0);
  val replacementTransitions = a1InitialTransitions.map{
    initialT -> glueStates.map{qG -> Transition(qG, initialT.verb, initialT.q2)}
  }.flatten();

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

fun genAutomaton(t: LocalType.InvocationRecv, c: Cache): SessionAutomaton {
  val q0 = c.nextState();
  val q1 = c.nextState();
  val r0 = c.registerForFuture(t.f);

  c.saveMethodForFuture(t.f, t.m);

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

fun genAutomaton(t: LocalType.Reactivation, c: Cache): SessionAutomaton {
  val q0 = c.nextState();
  val q1 = c.nextState();
  val r0 = c.registerForFuture(t.f);
  val method = c.findMethodForFuture(t.f)!!; // TODO convert to proper exception

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

fun genAutomaton(t: LocalType.Concatenation, c: Cache): SessionAutomaton {
  val a0 = genAutomaton(t.lhs, c);
  val a1 = genAutomaton(t.rhs, c);

  return concatAutomata(
    a0,
    a0.finalStates,
    a1,
    a1.finalStates
  );
}

fun genAutomaton(t: LocalType.Repetition, c: Cache): SessionAutomaton {
  // TODO: Rephrase as special case of automaton concatenation
  val a = genAutomaton(t.repeatedType, c);

  val initialTransitions = a.transitionsForState(a.q0);
  val additionalBackTransitions =
    initialTransitions.map{
      initialT -> a.finalStates.map{qF -> Transition(qF, initialT.verb, initialT.q2)}
    }.flatten();

  return SessionAutomaton(
    a.Q,
    a.q0,
    a.Delta // Δ ∪ {(q, v, q') | q ∈ F1 ∧ (q0, v, q') ∈ ∆}
      .union(additionalBackTransitions),
    a.registers,
    a.finalStates.union(listOf(a.q0))
  )
}

fun genAutomaton(t: LocalType.Branching, c: Cache): SessionAutomaton {
  if (t.choices.isEmpty()) {
    val q0 = c.nextState();

    return SessionAutomaton(
      setOf(q0),
      q0,
      emptySet(),
      emptySet(),
      setOf(q0) // TODO: Evaluate if this a sane decision, it requires special handling when merging branches.
    );
  }

  else {
    val head = t.choices.first()!!;
    val tail = t.choices.drop(1)!!;

    val headAutomaton = genAutomaton(head, c);
    val tailAutomaton = genAutomaton(
      LocalType.Branching(tail),
      c
    );

    // TODO: Enforce determinism
    // Either here or by providing a generic transformation function to generate
    // deterministic Session Automata from non-deterministic ones.
    // Or dont allow non deterministic types or transform types into
    // deterministic ones.

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

fun genAutomaton(t: LocalType, c: Cache) =
  when (t) { // TODO use visitor pattern instead?
    is LocalType.Branching      -> genAutomaton(t, c)
    is LocalType.Concatenation  -> genAutomaton(t, c)
    is LocalType.Reactivation   -> genAutomaton(t, c)
    is LocalType.Repetition     -> genAutomaton(t, c)
    is LocalType.InvocationRecv -> genAutomaton(t, c)
  }


fun genAutomaton(t: LocalType): SessionAutomaton {
  val c = Cache();

  return genAutomaton(t, c)
}
