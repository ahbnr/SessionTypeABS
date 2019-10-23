package de.ahbnr.sessiontypeabs.cli

import de.ahbnr.sessiontypeabs.compiler.CompilerConfig
import de.ahbnr.sessiontypeabs.compiler.compile
import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.types.analysis.model.VerificationConfig
import picocli.CommandLine.*

@Command(
    name = "compile"
)
class Compile : Runnable {
    @Parameters(paramLabel="FILES", arity="1..*")
    private val files: Array<String> = emptyArray()

    @Option(names = ["--logSchedulerCalls"])
    private val logSchedulerCalls: Array<String> = emptyArray()

    @Option(names = ["--logActivationDelay"])
    private val logActivationDelay: Array<String> = emptyArray()

    @Option(names = ["--noEnforcement"])
    private var noEnforcement: Boolean = false

    @Option(names = ["--noStaticChecks"])
    private var noStaticChecks: Boolean = false

    @Option(names = ["--verboseErlangCompilation"])
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
