package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.exceptions.CompilerException
import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ProjectionException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
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
        is TransferException -> "The following error occurred while validating a global type:"
        else -> "The following error occurred during compilation:"
    }

    val footer = when {
        exception is GlobalTypeException && exception.type.fileContext != null -> {
            val context = exception.type.fileContext!!

            "The error is located in file \"${context.file}\" at (${context.startLine}, ${context.startColumn})"
        }

        else -> "The location of the error could not be traced to a specific source file location."
    }

    println("$header\n\n${exception.message}\n\n$footer")
}

fun handleException(exception: Exception) =
    when (exception) {
        is CompilerException -> handleCompilerException(exception)
        else -> {
            println("An internal compiler error occurred:\n\n${exception.message}\n\n")
            exception.printStackTrace()
        }
    }
