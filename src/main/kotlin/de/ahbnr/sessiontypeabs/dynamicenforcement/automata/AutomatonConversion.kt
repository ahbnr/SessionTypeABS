package de.ahbnr.sessiontypeabs.dynamicenforcement.automata

import de.ahbnr.sessiontypeabs.removeSomeElement

fun nfaToDfa(automaton: SessionAutomaton): SessionAutomaton {
    val q0Meta = epsilonClosure(automaton.q0, automaton)
    val QMeta = mutableSetOf<Set<Int>>()
    val DeltaMeta = mutableSetOf<MetaTransition>()

    val toProcess = mutableSetOf(q0Meta)

    while (toProcess.isNotEmpty()) {
        val qMeta = toProcess.removeSomeElement()
        val DeltaPlus = groupedTransitions(qMeta, automaton)

        QMeta.add(qMeta)
        DeltaMeta.addAll(DeltaPlus)

        toProcess.addAll(
            DeltaPlus
                .filter { it.q2Meta !in QMeta }
                .map { it.q2Meta }
        )
    }

    val FMeta = QMeta
        .filter { qMeta -> qMeta.any { it in automaton.finalStates } }
        .toSet()

    return metaAutomatonToSessionAutomaton(
        QMeta = QMeta,
        q0Meta = q0Meta,
        DeltaMeta = DeltaMeta,
        FMeta = FMeta,
        originalAutomaton = automaton
    )
}

private fun metaAutomatonToSessionAutomaton(
    QMeta: Set<Set<Int>>,
    q0Meta: Set<Int>,
    DeltaMeta: Set<MetaTransition>,
    FMeta: Set<Set<Int>>,
    originalAutomaton: SessionAutomaton
): SessionAutomaton {
    val stateMapping = QMeta
        .mapIndexed { idx, qMeta -> qMeta to idx}
        .toMap()

    val Delta = DeltaMeta
        .map { (q1Meta, verb, q2Meta) ->
            Transition(
                q1 = stateMapping[q1Meta]!!,
                verb = verb,
                q2 = stateMapping[q2Meta]!!
            )
        }
        .toSet()

    return SessionAutomaton(
        Q = QMeta.map { stateMapping[it]!! }.toSet(),
        q0 = stateMapping[q0Meta]!!,
        Delta = Delta,
        finalStates = FMeta.map { stateMapping[it]!! }.toSet()
    )
}

private data class MetaTransition (
    val q1Meta: Set<Int>,
    val verb: TransitionVerb,
    val q2Meta: Set<Int>
)

private fun epsilonClosure(q: Int, automaton: SessionAutomaton): Set<Int> {
    val closure = mutableSetOf<Int>()
    val toProcess = mutableSetOf(q)

    while (toProcess.isNotEmpty()) {
        val q1 = toProcess.removeSomeElement()
        val closureExtension = automaton.Delta
            .filter { it.verb is TransitionVerb.Epsilon && it.q1 == q1 && it.q2 !in closure && it.q2 != q1}
            .map { it.q2 }
            .toSet()

        closure.addAll(closureExtension)
        closure.add(q1)

        toProcess.addAll(closureExtension)
    }

    return closure
}

private fun groupedTransitions(qMeta: Set<Int>, automaton: SessionAutomaton): Set<MetaTransition> =
    automaton.Delta
        .filter { it.q1 in qMeta && it.verb !is TransitionVerb.Epsilon }
        .groupBy { it.verb }
        .map { (verb, transitions) ->
            MetaTransition(
                q1Meta = qMeta,
                verb = verb,
                q2Meta = transitions
                    .map { epsilonClosure(it.q2, automaton) }
                    .fold(emptySet<Int>(), { accum, next -> accum union next })
                    .toSet()
            )
        }
        .toSet()

/**
 * Does not work correctly if there are epsilon transitions
 */
fun mergeInvocREv(automaton: SessionAutomaton): SessionAutomaton {
    val regsToNewReg = automaton.Delta
        .filter { it.verb is TransitionVerb.InvocREv && it.verb.postCondition == null }
        .groupBy { Pair(it.q1, (it.verb as TransitionVerb.InvocREv).method) }
        .map { (_, sameInvocs) -> sameInvocs.map { (it.verb as TransitionVerb.InvocREv).register } }
        .associateWith { it.first() }
        .entries

    fun mergeReg(r: Int) = regsToNewReg.find {
            (mergedRegs, newReg) -> r in mergedRegs
    }
        ?.value
        ?: r

    return automaton.copy(
        Delta = automaton.Delta.map { it.copy(
            verb = when (it.verb) {
                is TransitionVerb.InvocREv -> it.verb.copy(register = mergeReg(it.verb.register))
                is TransitionVerb.ReactEv -> it.verb.copy(register = mergeReg(it.verb.register))
                is TransitionVerb.Epsilon -> it.verb
            }
        )
        }.toSet()
    )
}
