package de.ahbnr.sessiontypeabs.abstoolsmods

import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.ast.ASTNode
import java.io.PrintWriter
import java.io.StringWriter

inline fun ASTNode<*>.oneshotPrettyPrint(): String {
    val stringWriter = StringWriter()
    val printer = PrintWriter(stringWriter)
    val formatter = DefaultABSFormatter(printer)

    this.doPrettyPrint(printer, formatter)
    printer.println()
    printer.println()

    printer.flush()

    return stringWriter.toString()
}

