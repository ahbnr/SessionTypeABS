package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.parseTypes

import picocli.CommandLine.*

@Command(
    name = "printTypes"
)
class PrintTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        try {
            val types = parseTypes(files.asIterable())

            println(types)
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
