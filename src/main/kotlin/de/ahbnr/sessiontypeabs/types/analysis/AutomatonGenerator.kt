package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.CondensedTypeVisitor
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method

fun genAutomaton(t: CondensedType) =
    genAutomaton(t, Cache())

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
 * @param finalStates states which shall act as final states of the resulting automaton.
 *                    If set to null, the algorithm will decide automatically on the final states:
 *                      a0.finalStates, if concatenation result has no transitions between the input automata
 *                                      (happens when concatenating skip automata)
 *                      a1.finalStates, if a1.q0 is not final
 *                      (a1.finalStates ∪ glueStates) \ {a1.qo}, otherwise, since the glue states also take a1.q0's
 *                                                               place as final states
 * @return concatenation of a0 and a1 at the glue states
 * @throws IllegalArgumentException if the glue states are not part of [a1]
 */
private fun concatAutomata(a0: SessionAutomaton, glueStates: Set<Int>, a1: SessionAutomaton, finalStates: Set<Int>? = null): SessionAutomaton {
    if (!a0.Q.containsAll(glueStates)) {
        throw IllegalArgumentException("To concatenate automata the glue states must all be part of the first automaton")
    }

    val a1InitialTransitions = a1.transitionsForState(a1.q0)
    val replacementTransitions = a1InitialTransitions.map{
            initialT -> glueStates.map{qG ->
        Transition(
            qG,
            initialT.verb,
            initialT.q2
        )
    }
    }.flatten()

    val resultingDelta =
        a0.Delta // Δ1 ∪ (Δ2 \ {(a1.q0, <...>)}) ∪ {(q, <...>) | q ∈ F1}
            .union(a1.Delta.minus(a1InitialTransitions))
            .union(replacementTransitions)

    // If a1.q0 is final, the glue states which replace a1.q0 must be final too
    val additionalFinalStates =
        if (a1.q0 in a1.finalStates) {
            glueStates
        }

        else {
            emptySet()
        }

    return SessionAutomaton(
        (a0.Q union a1.Q union additionalFinalStates) - a1.q0, // Q1 ∪ Q2 ∪ Q' \ {a1.q0} where Q' = glueStates, if a1.q0 was final
        a0.q0,
        resultingDelta,
        a0.registers union a1.registers,
        finalStates ?: if (replacementTransitions.isEmpty()) {
            a0.finalStates
        }

        else {
            (a1.finalStates union additionalFinalStates) - a1.q0
        }
    )
}

private fun genAutomaton(t: CondensedType.InvocationRecv, c: Cache): SessionAutomaton {
    val q0 = c.nextState()
    val q1 = c.nextState()
    val r0 = c.registerForFuture(t.f)

    c.saveMethodForFuture(t.f, t.m)

    return SessionAutomaton(
        setOf(q0, q1),
        q0,
        setOf(
            Transition(
                q0,
                TransitionVerb.InvocREv(
                    method = t.m,
                    register = r0,
                    postCondition = t.postCondition
                ),
                q1
            )
        ),
        setOf(r0),
        setOf(q1)
    )
}

private fun genAutomaton(t: CondensedType.Reactivation, c: Cache): SessionAutomaton {
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
            Transition(
                q0,
                TransitionVerb.ReactEv(
                    method = method,
                    register = r0
                ),
                q1
            )
        ),
        setOf(r0),
        setOf(q1)
    )
}

private fun genAutomaton(@Suppress("UNUSED_PARAMETER") t: CondensedType.Skip, c: Cache): SessionAutomaton {
    val q0 = c.nextState()

    return SessionAutomaton(
        setOf(q0),
        q0,
        emptySet(),
        emptySet(),
        setOf(q0)
    )
}

private fun genAutomaton(t: CondensedType.Concatenation, c: Cache): SessionAutomaton {
    val a0 = genAutomaton(t.lhs, c)
    val a1 = genAutomaton(t.rhs, c)

    return concatAutomata(
        a0,
        a0.finalStates,
        a1
    )
}

private fun genAutomaton(t: CondensedType.Repetition, c: Cache): SessionAutomaton {
    val a = genAutomaton(t.repeatedType, c)

    val initialTransitions = a.transitionsForState(a.q0)
    val additionalBackTransitions =
        initialTransitions.map{
                initialT -> a.finalStates.map{qF ->
            Transition(
                qF,
                initialT.verb,
                initialT.q2
            )
        }
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

private fun genAutomaton(t: CondensedType.Branching, c: Cache): SessionAutomaton {
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
            CondensedType.Branching(tail),
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

                else { // TODO extensive testing
                    // If the final states of the tailAutomaton contain its initial state, we have to replace it with the initial state of the headAutomaton,
                    // since that one replaces the initial state of the tailAutomaton.
                    tailAutomaton.finalStates - tailAutomaton.q0 + (
                            if (tailAutomaton.q0 in tailAutomaton.finalStates) {
                                setOf(headAutomaton.q0)
                            }

                            else {
                                emptySet()
                            }
                        )
                }
        )
    }
}

private fun genAutomaton(t: CondensedType, c: Cache) =
    t.accept(object: CondensedTypeVisitor<SessionAutomaton> {
        override fun visit(type: CondensedType.Reactivation) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.InvocationRecv) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Concatenation) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Repetition) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Branching) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Skip) =
            genAutomaton(type, c)
    })

