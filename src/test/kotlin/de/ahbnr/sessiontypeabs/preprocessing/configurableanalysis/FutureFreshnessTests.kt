package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.MergeException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.types.Future
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows

class FutureFreshnessTests {
    @Test
    fun `all used futures are remembered`() {
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

        assertThat(analyzedType.postState.getNonFreshFutures())
            .containsExactlyInAnyOrder(
                Future("f"),
                Future("fa"),
                Future("fb")
            )

        assertThat(analyzedType.postState.getActiveFutures())
            .isEmpty()
    }

    @Test
    fun `same future in different branches works`() {
        val type = loadType("""
            0 -f-> P:m.(
                P{
                    P -f2-> Q:m.Q resolves f2,
                    P -f2-> Q:m.Q resolves f2
                }
            )*.
            P resolves f
        """.trimIndent())

        val analyzedType = execute(CombinedAnalysis(), type)

        assertThat(analyzedType.postState.getNonFreshFutures())
            .containsExactlyInAnyOrder(
                Future("f"),
                Future("f2")
            )

        assertThat(analyzedType.postState.getActiveFutures())
            .isEmpty()
    }

    @Test
    fun `futures can not be introduced twice`() {
        val type = loadType("""
            0 -f-> P:m.(
                P -f2-> Q:m.
                    Q resolves f2
            )*.
            P -f2-> Q:m.
                Q resolves f2.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), type)
        }
    }

    @Test
    fun `only accessible futures can be read`() {
        val type = loadType("""
            0 -f-> P:m.
            P{
                P -f2-> Q:m.
                    Q resolves f2,
                P -f2-> Q:m.
                    Q resolves f2
            }.
            P fetches f2.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), type)
        }
    }

    @Test
    fun `the same future name in different branches is only allowed, if it binds to the same method`() {
        val type = loadType("""
            0 -f-> P:m.
            P{
                P -f2-> Q:m1.
                    Q resolves f2,
                P -f2-> Q:m2.
                    Q resolves f2
            }.
            P resolves f
        """.trimIndent())

        assertThrows<MergeException> {
            execute(CombinedAnalysis(), type)
        }
    }
}