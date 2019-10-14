package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.codegen.astmods.ModificationLog
import de.ahbnr.sessiontypeabs.codegen.astmods.enforceSessionTypesOnModel
import de.ahbnr.sessiontypeabs.compiler.exceptions.ABSException
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.analysis.model.checkModel
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.common.CompilerCondition
import org.abs_models.frontend.ast.Model
import org.abs_models.frontend.parser.Main
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*
import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.ast.ModuleDecl
import java.io.PrintWriter


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

private fun makeMain(): Main {
    val main = Main()
    main.arguments.verbose = true

    return main
}

fun parseModel(absSourceFileNames: Iterable<String>, noChecks: Boolean = false): Model {
    //val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val helperLib = getSchedulerLib()
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    val model = makeMain().parse(files)

    if (!noChecks) {
        checkAndRewriteModel(model)
        model.doFullTraversal() // make sure we triggered all rewrites
    }

    return model
}

/**
 * Parses the ABS source files and applies the given local Session Types on the
 * model, such that it complies to the protocol induced by them at runtime.
 */
fun buildModel(
    model: Model,
    typeBuilds: TypeBuildCollection,
    verificationConfig: VerificationConfig = VerificationConfig(),
    enforcementConfig: EnforcementConfig = EnforcementConfig()
): ModelBuild {
    checkAndRewriteModel(model)
    model.doFullTraversal() // make sure we triggered all rewrites

    if (!verificationConfig.noChecks) {
        // static verification
        for (typeBuild in typeBuilds.typeBuilds) {
            checkModel(model, typeBuild, verificationConfig)
        }
    }

    // Modify ABS model, if configuration doesnt forbid it
    val modLog = if (!enforcementConfig.noEnforcement) {
        val modLog = applyTypesToModel(
            model,
            typeBuilds.mergedCondensedTypes(),
            enforcementConfig
        )

        // Check whether all participants could be modified (none was missing in the model)
        checkForMissingParticipants(typeBuilds.mergedGlobalTypes(), modLog)

        modLog
    }

    else {
        ModificationLog()
    }

    val modifiedBeforeRewrite = model.treeCopyNoTransform()
    checkAndRewriteModel(model)

    return ModelBuild(
        model = model,
        modifiedModelBeforeRewrite = modifiedBeforeRewrite, // TODO: This one is only useful for printing the modified model, but a huge investment (requires copying the whole model). Maybe this can be optimized.
        modificationLog = modLog
    )
}

data class ModelBuild(
    val model: Model,
    val modifiedModelBeforeRewrite: Model,
    val modificationLog: ModificationLog
)

fun applyTypesToModel(model: Model, types: Map<Class, CondensedType>, enforcementConfig: EnforcementConfig = EnforcementConfig()) =
    enforceSessionTypesOnModel(model, types, enforcementConfig)

/**
 * Checks ABS model for parser, type errors etc. and prepares it for
 * compilation.
 *
 * FIXME: Throw fitting exception in case of errors
 */
fun checkAndRewriteModel(model: Model) {
    val main = makeMain()


    // FIXME: Reenable this. Originally the check method was not private...
    // main.analyzeFlattenAndRewriteModel(model)

    // dirty hack to access analysis functions, since they have been made private recently
    val m = Main::class.java.getDeclaredMethod("analyzeFlattenAndRewriteModel", Model::class.java)
    m.isAccessible = true
    m.invoke(main, model)

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
    ErlangBackend().compile(model, File("gen/erl/"), EnumSet.of(ErlangBackend.CompileOptions.VERBOSE))//EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
