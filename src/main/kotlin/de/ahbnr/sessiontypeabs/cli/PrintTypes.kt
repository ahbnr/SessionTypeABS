package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.parseTypes
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.parseFile
import picocli.CommandLine

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
