package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.types.Class
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat

class ParticipantsTests {
    @Test
    fun `all participants are collected`() {
        val type = loadType("""
            0 -f-> P:m.(
                P{
                    P -fa-> Q:m.Q resolves fa,
                    P -fb-> R:m.R resolves fb
                }
            )*.
            P resolves f
        """.trimIndent())

        val analyzedType = execute(CombinedAnalysis(), type)

        assertThat(analyzedType.postState.getParticipants())
            .containsExactlyInAnyOrder(
                Class("P"),
                Class("Q"),
                Class("R")
            )
    }
}