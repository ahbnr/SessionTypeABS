package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.compiler.parseTypes
import de.ahbnr.sessiontypeabs.types.intersperse

import picocli.CommandLine.*

@Command(
    name = "printCondensedLocalTypes"
)
class PrintCondensedLocalTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        try {
            val typeBuild = buildTypes(files.asIterable())

            println(
                typeBuild.condensedTypes.map { (actor, localType) ->
                    "${actor.value}:\n$localType"
                }
                    .intersperse("\n\n")
            )
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
