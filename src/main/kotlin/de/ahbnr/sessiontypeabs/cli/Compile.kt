package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compile
import picocli.CommandLine.*

@Command(
    name = "compile"
)
class Compile : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    override fun run() {
        extractUnknownFiles(files.asIterable())
            .forEach{
                println("WARNING: Not considering file $it, since it neither has the .abs or .st file extension")
            }

        val absSourceFiles = extractAbsSourceFiles(files.asIterable())
        val typeSourceFiles = extractTypeSourceFiles(files.asIterable())

        compile(absSourceFiles, typeSourceFiles)
    }
}
