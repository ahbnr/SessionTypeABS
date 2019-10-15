package de.ahbnr.sessiontypeabs.dynamicenforcement.automata

/**
 * Concatenates automata at given "glue" states by inserting an ε-Transition between the glue states
 * and the initial state of the second automaton.
 *
 * @param a0 first automaton
 * @param a1 second automaton
 * @param glueStates between these and the initial states of [a1], ε-Transition will be inserted.
 *                   if not set, the final states of [a1] are used.
 * @param finalStates states which shall act as final states of the resulting automaton.
 *                    If not set, the final states of [a1] will be used.
 * @return concatenation of a0 and a1 at the glue states
 * @throws IllegalArgumentException if the glue states are not part of [a1]
 */
fun concatAutomata(a0: SessionAutomaton, a1: SessionAutomaton, glueStates: Set<Int> = a0.finalStates, finalStates: Set<Int> = a1.finalStates): SessionAutomaton {
    require(a0.Q.containsAll(glueStates)) { "To concatenate automata the glue states must all be part of the first automaton" }
    require((a0.Q intersect a1.Q).isEmpty()) {"Can only concatenate automata which do not share states."}

    val epsilonTransitions = glueStates
        .map {
            Transition(
                q1 = it,
                verb = TransitionVerb.Epsilon,
                q2 = a1.q0
            )
        }

    return SessionAutomaton(
        Q = a0.Q union a1.Q, // Q1 ∪ Q2
        q0 = a0.q0,
        Delta = a0.Delta union a1.Delta + epsilonTransitions,
        registers = a0.registers union a1.registers,
        finalStates = finalStates
    )
}

infix fun SessionAutomaton.concat(rhs: SessionAutomaton) =
    concatAutomata(this, rhs)

fun repeatAutomaton(a: SessionAutomaton): SessionAutomaton {
    val additionalBackTransitions = a.finalStates
        .map { qF ->
            Transition(
                q1 = qF,
                verb = TransitionVerb.Epsilon,
                q2 = a.q0
            )
        }

    return SessionAutomaton(
        a.Q,
        a.q0,
        a.Delta // Δ ∪ {(qf, ε, q0) | qf ∈ F1 }
            union additionalBackTransitions,
        a.registers,
        a.finalStates
    )
}

infix fun SessionAutomaton.union(rhs: SessionAutomaton) =
    concatAutomata(
        a0 = this,
        a1 = rhs,
        glueStates = setOf(this.q0),
        finalStates = this.finalStates union rhs.finalStates
    )
