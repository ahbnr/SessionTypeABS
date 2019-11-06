package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ScopeClosingException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ResolutionTests {
    @Test
    fun `we can only fetch resolved futures`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
                Q resolves f2.
            P fetches f2.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            P fetches f2.
                Q resolves f2.
            P resolves f
        """.trimIndent())

        assertThrows<TransferException> {
            execute(CombinedAnalysis(), typeError)
        }
    }

    @Test
    fun `futures resolved in branchings can be read`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P -f2-> Q:m.
            Q {
                Q resolves f2,
                Q resolves f2
            }.
            P fetches f2.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)
    }

    @Test
    fun `futures are resolved within their scope`() {
        val typeOk = loadType("""
            0 -f-> P:m.
            P {
                P -f2-> Q:m.
                Q resolves f2
            }.
            P resolves f
        """.trimIndent())

        execute(CombinedAnalysis(), typeOk)

        val typeError = loadType("""
            0 -f-> P:m.
            P {
                P -f2-> Q:m
            }.
            Q resolves f2.
            P resolves f
        """.trimIndent())

        assertThrows<ScopeClosingException> {
            execute(CombinedAnalysis(), typeError)
        }
    }
}