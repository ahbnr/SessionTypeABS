package de.ahbnr.sessiontypeabs.cli

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
