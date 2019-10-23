package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.buildModel
import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.compiler.parseModel
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import org.abs_models.backend.prettyprint.DefaultABSFormatter

import picocli.CommandLine.*
import java.io.PrintWriter

@Command(
    name = "printModel"
)
class PrintModel : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    @Option(names = ["--logActivationDelay"])
    private val logActivationDelay: Array<String> = emptyArray()

    override fun run() {
        try {
            val absSourceFiles = files.filter {
                it.endsWith(".abs")
            }

            val typeSourceFiles = files.filter {
                it.endsWith(".st")
            }

            val model = parseModel(absSourceFiles)
            val typeBuild = buildTypes(typeSourceFiles, model)
            val modelBuild = buildModel(
                model,
                typeBuild,
                VerificationConfig(noChecks = true),
                EnforcementConfig(logActivationDelay = logActivationDelay.toSet())
            )

            val printer = PrintWriter(System.out)
            val formatter = DefaultABSFormatter(printer)

            modelBuild.modificationLog.allDecls().forEach {
                it.doPrettyPrint(printer, formatter)
                printer.println()
                printer.println()
            }

            printer.flush()
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
