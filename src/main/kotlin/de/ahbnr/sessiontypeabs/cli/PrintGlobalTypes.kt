package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.parseTypes

import picocli.CommandLine.*

@Command(
    name = "printGlobalTypes"
)
class PrintGlobalTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        try {
            val globalTypes = parseTypes(files.asIterable())

            println(globalTypes)
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
