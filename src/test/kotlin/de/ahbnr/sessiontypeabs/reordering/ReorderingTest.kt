package de.ahbnr.sessiontypeabs.reordering

import de.ahbnr.sessiontypeabs.compiler.*
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.scenarios.getTracingLib
import de.ahbnr.sessiontypeabs.tracing.*
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.fail
import java.io.File

fun performReorderingTest(fileHead: String, fileDir: String, noEnforcement: Boolean, expectedTraces: Map<Class, List<TraceFragment>>, whitelisted: Set<Method> = emptySet()) {
    val filePath = "reordering/$fileDir/$fileHead"

    val modelInput = ClassLoader.getSystemResourceAsStream("$filePath.abs")
    val modelFile = File.createTempFile(fileHead, ".abs")
    FileUtils.copyInputStreamToFile(modelInput, modelFile)

    val tracingLib = getTracingLib()

    val typeInput = ClassLoader.getSystemResourceAsStream("$filePath.st")
    val typeFile = File.createTempFile(fileHead, ".st")
    FileUtils.copyInputStreamToFile(typeInput, typeFile)

    val modelBuild = compile(
        listOf(modelFile.absolutePath, tracingLib.absolutePath),
        listOf(typeFile.absolutePath),
        VerificationConfig(
            noChecks = true
        ),
        EnforcementConfig(
            noEnforcement = noEnforcement,
            whitelistedMethods = setOf(Method("printTrace")) + whitelisted
        )
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
                expectedTraces,
                traces,
                "Output of model: $modelOutput"
            )
        }
    }
}

