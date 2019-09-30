package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.exceptions.ABSException
import de.ahbnr.sessiontypeabs.compiler.exceptions.CompilerException
import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.compiler.exceptions.LocalTypeException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.FinalizationException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ProjectionException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
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
        is ProjectionException -> "The following error occurred during projection:"
        is TransferException, is FinalizationException -> "The following error occurred while validating a global type:"
        is ParserException -> "The following error occurred while parsing type specifications:"
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

            "The error is located in file \"${context.file}\" at (${context.startLine}, ${context.startColumn})."
        }

        exception is ParserException ->
            "The error is located in file ${exception.fileName} at (${exception.line}, ${exception.column})."

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
