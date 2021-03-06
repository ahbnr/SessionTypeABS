package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.buildTypes
import de.ahbnr.sessiontypeabs.intersperse

import picocli.CommandLine.*

@Command(
    name = "printLocalTypes"
)
class PrintLocalTypes : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        try {
            val typeBuild = buildTypes(files.asIterable(), null)

            println(
                typeBuild.mergedLocalTypes().map { (actor, localType) ->
                    "${actor.value}:\n${localType.type}"
                }
                    .intersperse("\n\n")
            )
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
