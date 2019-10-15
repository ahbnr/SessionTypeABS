package de.ahbnr.sessiontypeabs.scenarios

import de.ahbnr.sessiontypeabs.compiler.*
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.tracing.*
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

class GradingSystem {
    @Test
    fun `grading system scenario`() {
        val modelInput = ClassLoader.getSystemResourceAsStream("scenarios/grading_system/GradingSystem.abs")
        val modelFile = File.createTempFile("GradingSystem", ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val tracingLib = getTracingLib()

        val typeInput = ClassLoader.getSystemResourceAsStream("scenarios/grading_system/GradingSystem.st")
        val typeFile = File.createTempFile("GradingSystem", ".st")
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

        val modelOutput = runModel("gen/erl/run")
        when (modelOutput) {
            is ModelRunResult.Timeout ->
                fail("Model execution timed out! There is likely a deadlock or bug.")
            is ModelRunResult.Error ->
                fail("""
                    Model execution failed with these errors:
                    ${modelOutput.stderr}
                    
                    Standard output contained this:
                    ${modelOutput.stdout}
                """)

            is ModelRunResult.Normal -> {
                val traces = processOutputToTraces(modelOutput.stdout)
                assertTraces(
                    mapOf(
                        Class("ComputationServer") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("ComputationServer"),
                                    method = Method("compute"),
                                    future = Future("f")
                                )
                            ),
                        Class("ServiceDesk") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("ServiceDesk"),
                                    method = Method("publish"),
                                    future = Future("publishTask")
                                ),
                                TraceFragment.Invocation(
                                    actor = Class("ServiceDesk"),
                                    method = Method("request"),
                                    future = Future("requestTask")
                                )
                            ),
                        Class("Student") to
                            listOf(
                                TraceFragment.Invocation(
                                    actor = Class("Student"),
                                    method = Method("announce"),
                                    future = Future("announceTask")
                                ),
                                TraceFragment.Reactivation(
                                    actor = Class("Student"),
                                    method = Method("announce"),
                                    future = Future("announceTask")
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
