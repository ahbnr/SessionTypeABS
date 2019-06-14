package de.ahbnr.sessiontypeabs.cli

import picocli.CommandLine

@CommandLine.Command(
    name = "SessionTypeABS",
    subcommands = arrayOf(
        Compile::class,
        PrintTypes::class,
        PrintModel::class
    )
)
class CLI : Runnable {
    override fun run() {
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            CommandLine.run(CLI(), System.err, *args)
        }
    }
}
