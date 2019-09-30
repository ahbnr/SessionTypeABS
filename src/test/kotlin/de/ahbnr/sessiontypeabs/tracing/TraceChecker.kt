package de.ahbnr.sessiontypeabs.tracing

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.intersperse
import org.assertj.core.api.Assertions.assertThat
import kotlin.math.min

fun assertTrace(actor: Class, expected: List<TraceFragment>, actual: List<TraceFragment>) {
    val futureIds = mutableMapOf<Future, Future>()

    fun makeDescriptionMessage(message: String) = """
            $message
            Expected trace: ${expected.mapIndexed{idx, fragment -> "$idx: $fragment"}.intersperse(", ")}
            Actual trace:   ${actual.mapIndexed{idx, fragment -> "$idx: $fragment"}.intersperse(", ")}
        """.trimMargin().trimIndent()

    val minTraceLength = min(expected.size, actual.size)
    for (idx in 0 until minTraceLength) {
        val expectedFragment = expected[idx]
        val actualFragment = actual[idx]

        fun makeIndexedDescriptionMessage(message: String) = """
${makeDescriptionMessage(message)}
            Error at position $idx
        """.trimMargin().trimIndent()

        assertThat(actualFragment)
            .describedAs(makeIndexedDescriptionMessage("Trace fragments are not of the same class."))
            .hasSameClassAs(expectedFragment)

        assertThat(actualFragment.actor)
            .describedAs(makeIndexedDescriptionMessage("Trace fragments do not belong to the same actor."))
            .isEqualTo(expectedFragment.actor)

        assertThat(actualFragment.method)
            .describedAs(makeIndexedDescriptionMessage("Trace fragments do not refer to the same method."))
            .isEqualTo(expectedFragment.method)

        when (actualFragment) {
            is TraceFragment.Invocation -> {
                assertThat(futureIds)
                    .describedAs(makeIndexedDescriptionMessage("There is an error in the expected trace fragment. The future ${expectedFragment.future.value} has already been created."))
                    .doesNotContainKey(expectedFragment.future)

                futureIds.put(expectedFragment.future, actualFragment.future)
            }

            else ->
                assertThat(actualFragment.future)
                    .describedAs(makeIndexedDescriptionMessage("The futures of the trace fragments do not match."))
                    .isEqualTo(futureIds[expectedFragment.future])
        }
    }

    assertThat(actual.size)
        .describedAs(makeDescriptionMessage("Recorded trace for ${actor.value} is not as long as expected."))
        .isEqualTo(expected.size)
}

fun assertTraces(expected: Map<Class, List<TraceFragment>>, actual: Map<Class, List<TraceFragment>>) {
    val expectedWithNonEmptyTrace = expected
        .filter { (_, trace) -> trace.isNotEmpty() }

    assertThat(actual.keys)
        .describedAs("Recorded traces do not cover the same actors as the expected traces.")
        .containsExactlyInAnyOrderElementsOf(expectedWithNonEmptyTrace.keys)

    for (actor in expectedWithNonEmptyTrace.keys) {
        assertTrace(actor, expected[actor]!!, actual[actor]!!)
    }
}