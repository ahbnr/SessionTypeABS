package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.CompilerConfig
import de.ahbnr.sessiontypeabs.compiler.compile
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.staticverification.VerificationConfig
import picocli.CommandLine.*

@Command(
    name = "compile",
    description = arrayOf("Statically verifies an ABS model against session types and apply dynamic enforcement modifications. Compiles the modified model to Erlang")
)
class Compile : Runnable {
    @Parameters(
        paramLabel="FILES",
        arity="1..*",
        description = arrayOf(".abs model files and .st session type files")
    )
    private val files: Array<String> = emptyArray()

    @Option(
        names = ["--logSchedulerCalls"],
        paramLabel = "ACTOR",
        description = arrayOf("Print a message, everytime the scheduler of the given actor is executed")
    )
    private val logSchedulerCalls: Array<String> = emptyArray()

    @Option(
        names = ["--logActivationDelay"],
        paramLabel = "ACTOR",
        description = arrayOf("Print a message, everytime the scheduler of the given actor can not select an activation to continue with.")
    )
    private val logActivationDelay: Array<String> = emptyArray()

    @Option(
        names = ["--noEnforcement"],
        description = arrayOf("Do not apply dynamic enforcement modifications.")
    )
    private var noEnforcement: Boolean = false

    @Option(
        names = ["--noStaticChecks"],
        description = arrayOf("Do not statically verify the ABS model against the session types.")
    )
    private var noStaticChecks: Boolean = false

    @Option(
        names = ["--verboseErlangCompilation"],
        description = arrayOf("Tell the ABS erlang backend compiler to be verbose.")
    )
    private var verboseErlangCompilation: Boolean = false

    override fun run() {
        try {
            extractUnknownFiles(files.asIterable())
                .forEach {
                    println("WARNING: Not considering file $it, since it neither has the .abs or .st file extension")
                }

            val absSourceFiles = extractAbsSourceFiles(files.asIterable())
            val typeSourceFiles = extractTypeSourceFiles(files.asIterable())

            compile(
                absSourceFileNames = absSourceFiles,
                typeSourceFileNames = typeSourceFiles,
                verificationConfig = VerificationConfig(noChecks = noStaticChecks),
                enforcementConfig = EnforcementConfig(
                    noEnforcement = noEnforcement,
                    logSchedulerCalls = logSchedulerCalls.toSet(),
                    logActivationDelay = logActivationDelay.toSet()
                ),
                compilerConfig = CompilerConfig(verboseErlangCompilation=verboseErlangCompilation)
            )
        }

        catch (exception: Exception) {
            handleException(exception)
        }
    }
}
