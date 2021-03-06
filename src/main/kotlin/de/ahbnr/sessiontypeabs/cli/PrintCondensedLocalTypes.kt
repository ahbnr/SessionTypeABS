package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.intersperse

import picocli.CommandLine.*

@Command(
    name = "printCondensedLocalTypes"
)
class PrintCondensedLocalTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        try {
            val typeBuilds = buildTypes(files.asIterable(), null)

            println(
                typeBuilds.mergedCondensedTypes().map { (actor, localType) ->
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
