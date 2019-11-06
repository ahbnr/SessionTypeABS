package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.MergeException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.SelfContainednessException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ActorActivityTests {
    @Test
    fun `we can not activate active actors`() {
        val type = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            P -f3-> Q:m.
            Q resolves f2.
            Q resolves f3.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), type)
        }
    }

    @Test
    fun `we can activate suspended actors`() {
        val type = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            Rel(P, f2).
            Q -f3-> P:m.
            P resolves f3.
            Q resolves f2.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), type)
    }

    @Test
    fun `we can not resolve a future an actor P is waiting on, if P is still active`() {
        val type = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            Rel(P, f2).
            Q -f3-> P:m.
            Q resolves f2.
            P resolves f3.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), type)
        }
    }

    @Test
    fun `only active actors can fetch futures`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
                Q resolves f2.
            P -f3-> Q:m.
            Rel(P, f3).
                Q resolves f3.
            P fetches f2.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
                Q resolves f2.
            P -f3-> Q:m.
            Rel(P, f3).
            P fetches f2.
                Q resolves f3.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), typeError)
        }
    }

    @Test
    fun `only active actors can call methods`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
                Q resolves f2.
            P -f3-> Q:m.
            Rel(P, f3).
                Q resolves f3.
            P -f4-> Q:m.
                Q resolves f4.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
                Q resolves f2.
            P -f3-> Q:m.
            Rel(P, f3).
            P -f4-> Q:m.
                Q resolves f3.
                Q resolves f4.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), typeError)
        }
    }

    @Test
    fun `no two processes in an actor may wait for the same future`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            Rel(P, f2).
                Q -f3->P:m.
                P -f4->R:m.
                Rel(P, f4).
                Q resolves f2.
            P resolves f.
                    R resolves f4.
                    P resolves f3
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            Rel(P, f2).
                Q -f3->P:m.
                Rel(P, f2).
                Q resolves f2.
                    P resolves f3.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), typeError)
        }
    }

    @Test
    fun `actors may release control on the same future in different branches`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            P{
                Rel(P, f2),
                Rel(P, f2)
            }.
                Q resolves f2.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)
    }

    @Test
    fun `actors must be in the same activity state after a branching`() {
        val type = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            P{
                Rel(P, f2),
                skip
            }.
                Q resolves f2.
            P resolves f
        """.trimIndent())

        assertThrows<MergeException> {
            execute(CombinedAnalysis(), type)
        }
    }

    @Test
    fun `an actor may not release control in a loop and not also be reactivated within it`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            (
                P -f2-> Q:m.
                Rel(P, f2).
                    Q resolves f2
            )*.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            (
                Rel(P, f2)
            )*.
                Q resolves f2.
            P resolves f
        """.trimIndent())

        assertThrows<SelfContainednessException> {
            execute(CombinedAnalysis(), typeError)
        }
    }
}