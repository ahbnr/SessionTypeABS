package de.ahbnr.sessiontypeabs

import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.parseFile

import picocli.CommandLine
import picocli.CommandLine.*

@CommandLine.Command(
    name = "SessionTypeABS"
)
class CLI : Runnable {
    // TODO make files parameter mandatory
    @Parameters(paramLabel="FILES")
    private val files: Array<String> = emptyArray()

    override fun run() {
        val absSourceFiles = files.filter{
            it.endsWith(".abs")
        }

        val typeSourceFiles = files.filter{
            it.endsWith(".st")
        }


        val mergedTypeInformation =
            typeSourceFiles
                .map{ parseFile(it) }
                .fold(emptyMap<String, LocalType>()) {
                        acc, element -> acc.plus(element)
                }

        // TODO: Check if any type files have been supplied at all
        println("Type information:")
        println(mergedTypeInformation)

        // TODO: Check if any ABS files have been supplied at all
        println("Modification of ABS model and compilation:")

        compile(absSourceFiles, mergedTypeInformation)
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CommandLine.run(CLI(), System.err, *args)
        }
    }
}
