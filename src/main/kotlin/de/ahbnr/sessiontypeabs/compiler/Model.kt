package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.codegen.astmods.ModificationLog
import de.ahbnr.sessiontypeabs.codegen.astmods.enforceSessionTypesOnModel
import de.ahbnr.sessiontypeabs.compiler.exceptions.ABSException
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.CondensedType
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.common.CompilerCondition
import org.abs_models.frontend.ast.Model
import org.abs_models.frontend.parser.Main
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

/**
 * It is not possible to create a readable file object from jar resources.
 * Thus we have to extract the scheduler library into a temporary file, because
 * the ABS compiler interface requires a file object.
 */
fun getSchedulerLib(): File {
    val inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("schedulerlib.abs")
    val file = File.createTempFile("schedulerlib", ".abs")

    FileUtils.copyInputStreamToFile(inputStream, file)

    return file
}

fun parseModel(absSourceFileNames: Iterable<String>): Model {
    //val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val helperLib = getSchedulerLib()
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    return Main.parseFiles(true, files)
}

/**
 * Parses the ABS source files and applies the given local Session Types on the
 * model, such that it complies to the protocol induced by them at runtime.
 */
fun buildModel(absSourceFileNames: Iterable<String>, typeBuild: TypeBuild, noChecks: Boolean = false): ModelBuild {
    val model = parseModel(absSourceFileNames)
    if (!noChecks) {
        checkAndRewriteModel(model)
    }

    // Modify ABS model
    val modLog = applyTypesToModel(model, typeBuild.condensedTypes)

    if (!noChecks) {
        // Check whether all participants could be modified (none was missing in the model)
        checkForMissingParticipants(typeBuild.analyzedProtocols, modLog)

        checkAndRewriteModel(model)
    }

    return ModelBuild(
        model = model,
        modificationLog = modLog
    )
}

data class ModelBuild(
    val model: Model,
    val modificationLog: ModificationLog
)

fun applyTypesToModel(model: Model, types: Map<Class, CondensedType>) =
    enforceSessionTypesOnModel(model, types)

/**
 * Checks ABS model for parser, type errors etc. and prepares it for
 * compilation.
 *
 * FIXME: Throw fitting exception in case of errors
 */
fun checkAndRewriteModel(model: Model) {
    val parser = Main()
    parser.analyzeFlattenAndRewriteModel(model)

    handleModelErrors(model)
}

fun handleModelErrors(model: Model) {
    when {
        model.hasParserErrors() -> throw ABSException(
            errors = model.parserErrors,
            message = "The ABS compiler reported parser errors."
        )
        model.hasErrors() -> throw ABSException(
            errors = model.errors.filterIsInstance<CompilerCondition>(),
            message = "The ABS compiler reported errors."
        )
        model.hasTypeErrors() -> throw ABSException(
            errors = model.typeErrors.filterIsInstance<CompilerCondition>(),
            message = "The ABS compiler reported type errors."
        )
    }
}

/**
 * Compiles ABS model to Erlang
 */
fun modelToErlang(model: Model) =
    ErlangBackend().compile(model, File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
