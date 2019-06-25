package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.compiler.exceptions.CompilerException
import org.antlr.v4.runtime.RuleContext

class ParserException(
    val fileName: String,
    val line: Int,
    val column: Int,
    message: String
): CompilerException(message)
