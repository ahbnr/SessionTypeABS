package de.ahbnr.sessiontypeabs

import de.ahbnr.sessiontypeabs.codegen.astmods.*
import de.ahbnr.sessiontypeabs.types.LocalType
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.parser.Main
import java.io.File

import java.io.PrintWriter
import java.util.*

/**
 * Modifies an ABS model, such that it complies to a given set of Local Session Types at runtime and then compiles them
 * to Erlang.
 *
 * Use [de.ahbnr.sessiontypeabs.types.parseFile] to parse the Session Types from files.
 */
fun compile(absSourceFileNames: List<String>, classToType: Map<String, LocalType>) {
    val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    // TODO make printing optional:
    val printer = PrintWriter(System.out)
    val formatter = DefaultABSFormatter(printer)

    try {
        val model = Main.parseFiles(true, files)

        enforceSessionTypesOnModel(model, classToType, printer, formatter)

        val parser = Main()
        parser.analyzeFlattenAndRewriteModel(model)
        if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) {
            System.out.println("Parsing failed.")
            return
        }

        ErlangBackend().compile(model, File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}