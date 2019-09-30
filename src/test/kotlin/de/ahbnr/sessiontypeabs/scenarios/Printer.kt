package de.ahbnr.sessiontypeabs.scenarios

import de.ahbnr.sessiontypeabs.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.compiler.*
import de.ahbnr.sessiontypeabs.generator.replicate
import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.tracing.assertTraces
import de.ahbnr.sessiontypeabs.tracing.processOutputToTraces
import de.ahbnr.sessiontypeabs.tracing.runModel
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.ast.ClassDecl
import org.abs_models.frontend.ast.MethodImpl
import org.abs_models.frontend.ast.MethodSig
import org.abs_models.frontend.ast.ModuleDecl
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration
import org.junit.jupiter.api.Test
import java.io.File
import java.io.PrintWriter

class Printer {
    @Test
    fun `printer scenario`() {
        val modelInput = ClassLoader.getSystemResourceAsStream("scenarios/printer/Printer.abs")
        val modelFile = File.createTempFile("Printer", ".abs")
        FileUtils.copyInputStreamToFile(modelInput, modelFile)

        val tracingLib = getTracingLib()

        val typeInput = ClassLoader.getSystemResourceAsStream("scenarios/printer/Printer.st")
        val typeFile = File.createTempFile("Printer", ".st")
        FileUtils.copyInputStreamToFile(typeInput, typeFile)

        //val model = parseModel(listOf(modelFile.absolutePath, tracingLib.absolutePath))
        val modelBuild = compile(listOf(modelFile.absolutePath, tracingLib.absolutePath), listOf(typeFile.absolutePath))

        //val printer = PrintWriter(System.out)
        //val formatter = DefaultABSFormatter(printer)

        //modelBuild.modifiedModelBeforeRewrite.findChildren<ModuleDecl>().find { it.name == "Printer" }?.let {
        //    it.doPrettyPrint(printer, formatter)
        //    printer.println()
        //    printer.println()
        //}

        //printer.flush()

        val modelOutput = runModel("gen/erl/run")
        assertThat(modelOutput)
            .describedAs("Model does not produce output, but it should print traces.")
            .isNotNull()

        val traces = processOutputToTraces(modelOutput!!)
        assertTraces(
            mapOf(
                Class("Client") to
                    listOf(
                        TraceFragment.Invocation(
                            actor = Class("Client"),
                            method = Method("main"),
                            future = Future("f")
                        )
                    ) +
                    replicate(
                        2 * 2 + 1, // requesting a print and awaiting its finalization 4 times. Plus another unsuccessful request
                        TraceFragment.Reactivation(
                            actor = Class("Client"),
                            method = Method("main"),
                            future = Future("f")
                        )
                    ),
                Class("Printer") to
                    listOf(
                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("preparePage"),
                            future = Future("pageRequest0")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("print"),
                            future = Future("printJob0")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("finishPage"),
                            future = Future("finalization0")
                        ),

                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("preparePage"),
                            future = Future("pageRequest1")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("print"),
                            future = Future("printJob1")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("finishPage"),
                            future = Future("finalization1")
                        ),

                        TraceFragment.Invocation(
                            actor = Class("Printer"),
                            method = Method("preparePage"),
                            future = Future("pageRequest2")
                        )
                    )
            ),
            traces
        )
    }
}

fun getTracingLib(): File {
    val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("tracinglib.abs")
    val file = File.createTempFile("tracinglib", ".abs")

    FileUtils.copyInputStreamToFile(inputStream, file)

    return file
}
