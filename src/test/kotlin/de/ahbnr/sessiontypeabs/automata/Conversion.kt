package de.ahbnr.sessiontypeabs.automata

import de.ahbnr.sessiontypeabs.dynamicenforcement.automata.*
import de.ahbnr.sessiontypeabs.types.Method
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.Test

class Conversion {
    @Test
    fun  `converting a deterministic automaton results in the same number of states etc`() {
        val t1 = Transition(0, TransitionVerb.InvocREv(Method("m1"), 0), 1)

        val automaton = SessionAutomaton(
            Q = setOf(0, 1, 2),
            q0 = 0,
            Delta = setOf(
                t1,
                Transition(1, TransitionVerb.InvocREv(Method("m2"), 1), 2),
                Transition(2, TransitionVerb.InvocREv(Method("m3"), 2), 1)
            ),
            finalStates = setOf(2)
        )

        val dfa = nfaToDfa(automaton)

        assertThat(dfa.Q)
            .hasSameSizeAs(automaton.Q)

        assertThat(dfa.finalStates)
            .hasSameSizeAs(automaton.finalStates)

        assertThat(dfa.registers)
            .hasSameSizeAs(automaton.registers)

        assertThat(dfa.Delta)
            .hasSameSizeAs(automaton.Delta)

        val (_, v1, _) = dfa.Delta.find { it.q1 == automaton.q0 }!!
        assertThat(v1)
            .isEqualTo(t1.verb)
    }

    @Test
    fun `no epsilon transitions in DFA of concatenated automaton`() {
        val (_, simpA1) = genSimpAutomaton(0)
        val (_, simpA2) = genSimpAutomaton(2)

        val automaton = simpA1 concat simpA2

        val dfa = nfaToDfa(automaton)

        assertThat(dfa.Delta.map(Transition::verb))
            .doesNotContain(TransitionVerb.Epsilon)

        assertThat(dfa.Q.size)
            .isEqualTo(automaton.Q.size - 1)

        assertThat(dfa.Delta)
            .noneMatch { it.q1 in dfa.finalStates }
    }

    @Test
    fun `no epsilon transitions in DFA of concatenated automaton with loops`() {
        val (v1, simpA1) = genSimpAutomaton(0)
        val (v2, simpA2) = genSimpAutomaton(2)

        val automaton = repeatAutomaton(simpA1 concat simpA2)
        val dfa = nfaToDfa(automaton)

        assertThat(dfa.Delta.map(Transition::verb))
            .doesNotContain(TransitionVerb.Epsilon)

        assertThat(dfa.Q.size)
            .isEqualTo(automaton.Q.size - 1)

        assertThat(dfa.finalStates)
            .hasSize(1)

        assertThat(dfa.Delta)
            .noneMatch { it.q2 == dfa.q0 }

        val transitionsOfFinalState = dfa.Delta.filter { it.q1 in dfa.finalStates }
        assertThat(transitionsOfFinalState)
            .hasSize(1)
        val (transitionOfFinalState) = transitionsOfFinalState

        val transitionsOfInitialState = dfa.Delta.filter { it.q1 == automaton.q0 }
        assertThat(transitionsOfInitialState)
            .hasSize(1)
        val (transitionOfInitialState) = transitionsOfInitialState

        assertThat(transitionOfFinalState.verb)
            .isEqualTo(transitionOfInitialState.verb)

        assertThat(transitionOfFinalState.q2)
            .isEqualTo(transitionOfInitialState.q2)
    }

    @Test
    fun `no epsilon transitions in DFA of union automaton`() {
        val (v1, simpA1) = genSimpAutomaton(0)
        val (v2, simpA2) = genSimpAutomaton(2)

        val automaton = simpA1 union simpA2
        val dfa = nfaToDfa(automaton)

        assertThat(dfa.Delta.map(Transition::verb))
            .doesNotContain(TransitionVerb.Epsilon)

        assertThat(dfa.Q.size)
            .isEqualTo(automaton.Q.size - 1)

        assertThat(dfa.finalStates)
            .hasSize(2)

        assertThat(dfa.Delta)
            .hasSize(2)

        assertThat(dfa.Delta)
            .noneMatch { it.q2 == dfa.q0 }

        assertThat(dfa.Delta)
            .noneMatch { it.q1 in dfa.finalStates }

        assertThat(dfa.Delta.map { it.verb })
            .containsExactlyInAnyOrderElementsOf(setOf(v1, v2))
    }

    @Test
    fun `union of automata with same initial transition merges that transition`() {
        val (v1, simpA1) = genSimpAutomaton(0)
        val (v2, simpA2) = genSimpAutomaton(2)
        val (v3, simpA3) = genSimpAutomaton(4)
        val (_, simpA4) = genSimpAutomaton(6)

        val simpA1mod = simpA4.copy(
            Delta = simpA4.Delta.map { it.copy(verb = v1) }.toSet()
        )

        val automaton = (simpA1 concat simpA2) union (simpA1mod concat simpA3)
        val dfa = nfaToDfa(automaton)

        assertThat(dfa.Delta.map(Transition::verb))
            .doesNotContain(TransitionVerb.Epsilon)

        assertThat(dfa.Q.size)
            .isEqualTo(automaton.Q.size - 4)

        assertThat(dfa.finalStates)
            .hasSize(2)

        assertThat(dfa.Delta)
            .hasSize(3)

        assertThat(dfa.Delta)
            .noneMatch { it.q2 == dfa.q0 }

        assertThat(dfa.Delta)
            .noneMatch { it.q1 in dfa.finalStates }

        assertThat(dfa.Delta.map { it.verb })
            .containsExactlyInAnyOrderElementsOf(setOf(v1, v2, v3))

        val transitionsIntoFinalStates = dfa.Delta.filter { it.q2 in dfa.finalStates }
        assertThat(transitionsIntoFinalStates)
            .hasSize(2)

        val transitionsOfInitialState = dfa.Delta.filter { it.q1 == automaton.q0 }
        assertThat(transitionsOfInitialState)
            .hasSize(1)
        val (transitionOfInitialState) = transitionsOfInitialState

        assertThat(transitionsIntoFinalStates.map{it.verb})
            .containsExactlyInAnyOrderElementsOf(setOf(v2, v3))

        assertThat(transitionOfInitialState.verb)
            .isEqualTo(v1)
    }

    private fun genSimpAutomaton(id: Int): Pair<TransitionVerb, SessionAutomaton> {
        val transition = Transition(id, TransitionVerb.InvocREv(Method("m$id"), id), id+1)

        return Pair(
            transition.verb,
            SessionAutomaton(
                Q = setOf(id, id + 1),
                q0 = id,
                Delta = setOf(
                    transition
                ),
                finalStates = setOf(id + 1)
            )
        )
    }
}