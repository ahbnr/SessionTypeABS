package de.ahbnr.sessiontypeabs

import de.ahbnr.sessiontypeabs.codegen.astmods.*
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.parseFile
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.frontend.parser.Main
import java.io.File

import java.util.*

import org.abs_models.frontend.ast.*

/**
 * Modifies an ABS model, such that it complies to a given set of Local Session Types at runtime and then compiles them
 * to Erlang.
 *
 * Use [de.ahbnr.sessiontypeabs.types.parseFile] to parse the Session Types from files.
 */
fun compile(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>) {
    val model = parseModel(absSourceFileNames)
    val types = parseTypes(typeSourceFileNames)

    applyTypesToModel(model, types)
    checkAndRewriteModel(model)

    modelToErlang(model)
}

fun parseModel(absSourceFileNames: Iterable<String>): Model {
    val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    return Main.parseFiles(true, files);
}

fun parseTypes(typeSourceFileNames: Iterable<String>): Map<String, LocalType> =
    typeSourceFileNames
        .map{ parseFile(it) }
        .fold(emptyMap<String, LocalType>()) { // Merge type information from each file
                acc, element -> acc.plus(element)
        }

fun applyTypesToModel(model: Model, types: Map<String, LocalType>) =
    enforceSessionTypesOnModel(model, types)

fun checkAndRewriteModel(model: Model) {
    val parser = Main()
    parser.analyzeFlattenAndRewriteModel(model)

    if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) {
        System.out.println("Parsing failed.")
    }
}

fun modelToErlang(model: Model) =
    ErlangBackend().compile(model, File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
