package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.applyTypesToModel
import de.ahbnr.sessiontypeabs.codegen.schedulerLibModuleName
import de.ahbnr.sessiontypeabs.parseModel
import de.ahbnr.sessiontypeabs.parseTypes
import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.ast.ModuleDecl

import picocli.CommandLine.*
import java.io.PrintWriter

@Command(
    name = "printModel"
)
class PrintModel : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        val absSourceFiles = files.filter{
            it.endsWith(".abs")
        }

        val typeSourceFiles = files.filter{
            it.endsWith(".st")
        }

        val model = parseModel(absSourceFiles)
        val types = parseTypes(typeSourceFiles)

        val modifiedDecls = applyTypesToModel(model, types)

        val printer = PrintWriter(System.out)
        val formatter = DefaultABSFormatter(printer)

        modifiedDecls.forEach{
            it.doPrettyPrint(printer, formatter)
            printer.println()
            printer.println()
        }

        printer.flush()
    }
}
