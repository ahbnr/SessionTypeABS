package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.parseTypes

import picocli.CommandLine.*

@Command(
    name = "printTypes"
)
class PrintTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        val types = parseTypes(files.asIterable())

        println(types)
    }
}
