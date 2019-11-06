package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.exceptions.ABSException
import de.ahbnr.sessiontypeabs.compiler.exceptions.CompilerException
import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.projection.exceptions.ProjectionException
import de.ahbnr.sessiontypeabs.types.parser.ParserException
import java.lang.Exception

fun extractAbsSourceFiles(files: Iterable<String>) =
    files.filter{
        it.endsWith(".abs")
    }

fun extractTypeSourceFiles(files: Iterable<String>) =
    files.filter{
        it.endsWith(".st")
    }

fun extractUnknownFiles(files: Iterable<String>) =
    files
        .filter{
            !it.endsWith(".abs") && !it.endsWith(".st")
        }

fun handleCompilerException(exception: CompilerException) {
    val header = when(exception) {
        is ParserException -> "The following error occurred while parsing type specifications:"
        is ConfigurableAnalysisException -> "The following error occurred while validating a global type:"
        is ProjectionException -> "The following error occurred during projection:"
        is ModelAnalysisException -> "The following error occurred during static verification:"
        else -> "The following error occurred during compilation:"
    }

    val message = when(exception) {
        is ABSException -> {
                val errorListing = exception.errors.fold("") {
                    acc, nextError -> "$acc\n$nextError"
                }

                "${exception.message}\n$errorListing\n"
            }
        else -> exception.message
    }

    val footer = when {
        exception is GlobalTypeException && exception.type.fileContext != null -> {
            val context = exception.type.fileContext!!

            genLocationMsg(context.file, context.startLine, context.startColumn)
        }

        exception is ConfigurableAnalysisException && exception.type.fileContext != null -> {
            val context = exception.type.fileContext!!

            genLocationMsg(context.file, context.startLine, context.startColumn)
        }

        exception is ParserException ->
            genLocationMsg(exception.fileName, exception.line, exception.column)

        exception is ABSException -> ""

        else -> "The location of the error could not be traced to a specific source file location."
    }

    println("$header\n\n$message\n\n$footer")
}

fun handleException(exception: Exception) =
    when (exception) {
        is CompilerException -> handleCompilerException(exception)
        else -> {
            println("An internal compiler error occurred:\n\n${exception.message}\n\n")
            exception.printStackTrace()
        }
    }

private fun genLocationMsg(fileName: String, line: Int, column: Int) =
    "The error is located in file \"$fileName\" at line $line, column $column."
