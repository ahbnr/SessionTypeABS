package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.compiler.buildModel
import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.compiler.modelToErlang
import de.ahbnr.sessiontypeabs.tracing.assertTraces
import de.ahbnr.sessiontypeabs.tracing.processOutputToTraces
import de.ahbnr.sessiontypeabs.tracing.runModel
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.junit.jupiter.api.Test
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import kotlin.random.Random
import com.pholser.junit.quickcheck.Property
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck
import org.junit.jupiter.api.TestInstance
import org.junit.runner.RunWith


//@RunWith(JUnitQuickcheck::class)
class Test {
    private fun saveInTempFile(baseFileName: String, fileExtension: String, data: String): File {
        val file = File.createTempFile(baseFileName, ".$fileExtension")

        val bw = BufferedWriter(FileWriter(file))
        bw.write(data)
        bw.close()

        return file
    }

    //@Property(shrink = false)
    @Test
    fun test() {
        // TODO better seed generator
        val random = java.util.Random()

        for (i in (1 .. 40)) {
            println("\nGeneration iteration $i\n")
            generateAndTrace(random.nextInt())
        }
    }

    fun generateAndTrace(seed: Int) {
        println("Seed: $seed")

        val result = generate(
            RandomSource(
                RandomSourceConfig(
                    seed = seed,
                    stepProbability = 0.9,
                    maxSteps = 20,
                    methodReuseProbability = 0.5,
                    actorReuseProbability = 0.5
                )
            )
        )

        println(result.protocol)
        //println(result.model)

        val modelFile = saveInTempFile("generated", "abs", result.model)

        val typeBuild = buildTypes(listOf(result.protocol))
        val modelBuild = buildModel(listOf(modelFile.absolutePath), typeBuild)

        //val printer = PrintWriter(System.out)
        //val formatter = DefaultABSFormatter(printer)

        //modelBuild.modificationLog.allDecls().forEach {
        //    it.doPrettyPrint(printer, formatter)
        //    printer.println()
        //    printer.println()
        //}

        //printer.flush()

        modelToErlang(modelBuild.model)

        //println(System.getProperty("user.dir"));

        val modelOutput = runModel("gen/erl/run")
        //println(modelOutput)

        val traceRecordings = processOutputToTraces(modelOutput)

        assertTraces(result.traces, traceRecordings)

        //println(result.traces)
    }
}