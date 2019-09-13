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

fun runModel(executablePath: String, timeoutSeconds: Long = 120): CharSequence? { // Default timeout: 2min
    val pb = NuProcessBuilder(listOf(executablePath))

    val processHandler = object: NuAbstractProcessHandler() {
        val output = StringBuilder()

        override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            output.append(String(bytes))
        }
    }

    pb.setProcessListener(processHandler)

    val process = pb.start()
    val returnValue = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

    return if (returnValue == Integer.MIN_VALUE) { // has the timeout been reached?
        null
    }

    else {
        processHandler.output.toString()
    }
}