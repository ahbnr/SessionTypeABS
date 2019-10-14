package de.ahbnr.sessiontypeabs.tracing

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcessBuilder
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

fun toTraceFragment(inputLine: String): TraceFragment? {
    val split = inputLine.split(' ')

    return if (split.size == 5 && split[0] == "[TRACE]") {
        when (split[1]) {
            "Invocation" -> TraceFragment.Invocation(
                future = Future(split[2]),
                actor = Class(split[3]),
                method = Method(split[4])
            )

            "Reactivation" -> TraceFragment.Reactivation(
                future = Future(split[2]),
                actor = Class(split[3]),
                method = Method(split[4])
            )

            else -> null
        }
    }

    else {
        null
    }
}

fun processOutputToTraces(output: CharSequence) =
    output
        .lines()
        .mapNotNull { toTraceFragment(it) }
        .groupBy { it.actor }

sealed class ModelRunResult {
    data class Normal(val stdout: CharSequence): ModelRunResult()
    data class Timeout(
        val stdout: CharSequence,
        val stderr: CharSequence
    ): ModelRunResult()
    data class Error(
        val stdout: CharSequence,
        val stderr: CharSequence
    ): ModelRunResult()
}

fun runModel(executablePath: String, timeoutSeconds: Long = 120): ModelRunResult { // Default timeout: 2min
    val pb = NuProcessBuilder(listOf(executablePath))

    val processHandler = object: NuAbstractProcessHandler() {
        val stdout = StringBuilder()
        var stderr = StringBuilder()

        override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            stdout.append(String(bytes))
        }

        override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            stderr.append(String(bytes))
        }
    }

    pb.setProcessListener(processHandler)

    val process = pb.start()
    val returnValue = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

    val stdout = processHandler.stdout.toString()
    val stderr = processHandler.stderr.toString()

    return when {
        returnValue == Int.MIN_VALUE -> ModelRunResult.Timeout(
            stdout = stdout,
            stderr = stderr
        )
        "AssertionFailException" in stderr -> ModelRunResult.Error( // I would rather check stderr, but ABS uses stderr for everything but println
            stdout = stdout,
            stderr = stderr
        )
        else -> ModelRunResult.Normal(processHandler.stdout.toString())
    }
}