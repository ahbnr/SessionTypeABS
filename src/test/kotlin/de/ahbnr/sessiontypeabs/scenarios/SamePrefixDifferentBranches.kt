package de.ahbnr.sessiontypeabs.scenarios

import de.ahbnr.sessiontypeabs.compiler.*
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.tracing.*
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import org.apache.commons.io.FileUtils
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

// Disabled this test for now, since we simply accept the fact, that not all generated automata are data-deterministic
class SamePrefixDifferentBranches {
    @Ignore
    fun `when having the same prefix in different session type branches, but one is longer, the scheduler should not get stuck when executing the second branch`() {
        val fileHead = "SamePrefixDifferentBranches"
        val fileDir = "same_prefix_different_branches"

        val modelInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead.abs")
        val modelFile = File.createTempFile(fileHead, ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val tracingLib = getTracingLib()

        val typeInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead.st")
        val typeFile = File.createTempFile(fileHead, ".st")
        FileUtils.copyInputStreamToFile(typeInput, typeFile)

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
                        Class("P") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("P"),
                                    method = Method("m"),
                                    future = Future("f")
                                )
                            ),
                        Class("Q") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("Q"),
                                    method = Method("m"),
                                    future = Future("fb")
                                ),
                                TraceFragment.Invocation(
                                    actor = Class("Q"),
                                    method = Method("m"),
                                    future = Future("fb2")
                                )
                            )
                    ),
                    traces,
                    "Output of model: $modelOutput"
                )
            }
        }
    }

    @Ignore
    fun `when renaming registers while merging invocations, invocations in reactivations must also be properly renamed`() {
        val fileHead = "SamePrefixDifferentBranches2"
        val fileDir = "same_prefix_different_branches"

        val modelInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead.abs")
        val modelFile = File.createTempFile(fileHead, ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val tracingLib = getTracingLib()

        val typeInput = ClassLoader.getSystemResourceAsStream("scenarios/$fileDir/$fileHead.st")
        val typeFile = File.createTempFile(fileHead, ".st")
        FileUtils.copyInputStreamToFile(typeInput, typeFile)

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
                        Class("P") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("P"),
                                    method = Method("m"),
                                    future = Future("f")
                                )
                            ),
                        Class("Q") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("Q"),
                                    method = Method("m"),
                                    future = Future("fb")
                                ),
                                TraceFragment.Reactivation(
                                    actor = Class("Q"),
                                    method = Method("m"),
                                    future = Future("fb")
                                )
                            ),
                        Class("R") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("R"),
                                    method = Method("m"),
                                    future = Future("fb2")
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
