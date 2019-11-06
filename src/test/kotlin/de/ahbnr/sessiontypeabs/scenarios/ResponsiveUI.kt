package de.ahbnr.sessiontypeabs.scenarios

import de.ahbnr.sessiontypeabs.compiler.*
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.tracing.*
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.staticverification.VerificationConfig
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import java.io.File
import java.lang.AssertionError

class ResponsiveUI {
    @Test
    fun `responsive ui scenario with post-condition checking`() {
        performTest("")
    }

    @Test
    fun `responsive ui scenario with post-condition checking, where an assertion should fail`() {
        assertThrows<AssertionError> {
            performTest("WrongPostCond")
        }
    }

    private fun performTest(typeVersion: String) {
        val fileHead = "ResponsiveUI"
        val fileDir = "responsive_ui"

        val modelInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead.abs")
        val modelFile = File.createTempFile(fileHead, ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val tracingLib = getTracingLib()

        val typeInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead$typeVersion.st")
        val typeFile = File.createTempFile("$fileHead$typeVersion", ".st")
        FileUtils.copyInputStreamToFile(typeInput, typeFile)

        //val model = parseModel(listOf(modelFile.absolutePath, tracingLib.absolutePath))
        val modelBuild = compile(
            listOf(modelFile.absolutePath, tracingLib.absolutePath),
            listOf(typeFile.absolutePath),
            VerificationConfig(
                strictMain = false,
                shareableInterfaces = setOf("SessionTypeABS.Tracing.TraceableI")
            ),
            EnforcementConfig(whitelistedMethods = setOf(Method("printTrace")))
        )

        val modelOutput = runModel("gen/erl/run", timeoutSeconds = 5)

        when (modelOutput) {
            is ModelRunResult.Timeout ->
                fail("""
                    Model execution timed out! There is likely a deadlock or bug.
                    
                    Stderr:
                    ${modelOutput.stderr}
                    
                    Stdout:
                    ${modelOutput.stdout}
                """.trimIndent())
            is ModelRunResult.Error ->
                fail("""
                    Model execution failed with these errors:
                    ${modelOutput.stderr}
                    
                    Standard output contained this:
                    ${modelOutput.stdout}
                """)

            is ModelRunResult.Normal -> {
                println(modelOutput)

                val traces = processOutputToTraces(modelOutput.stdout)
                assertTraces(
                    mapOf(
                        Class("U") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("U"),
                                    method = Method("start"),
                                    future = Future("startTask")
                                ),
                                TraceFragment.Invocation(
                                    actor = Class("U"),
                                    method = Method("resume"),
                                    future = Future("resumeTask")
                                )
                            ),
                        Class("I") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("I"),
                                    method = Method("cmp"),
                                    future = Future("cmpITask")
                                ),
                                TraceFragment.Reactivation(
                                    actor = Class("I"),
                                    method = Method("cmp"),
                                    future = Future("cmpITask")
                                )
                            ),
                        Class("S") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("S"),
                                    method = Method("cmp"),
                                    future = Future("cmpSTask")
                                )
                            )
                    ),
                    traces,
                    "Output of model: $modelOutput"
                )
            }
        }
    }
}
