package de.ahbnr.sessiontypeabs.tracing

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import org.assertj.core.api.Assertions.assertThat

fun assertTrace(actor: Class, expected: List<TraceFragment>, actual: List<TraceFragment>) {
    val futureIds = mutableMapOf<Future, Future>()

    fun makeDescriptionMessage(message: String) = """
        $message
        Expected trace: $expected
        Actual trace: $actual
    """.trimMargin().trimIndent()

    assertThat(actual.size)
        .describedAs(makeDescriptionMessage("Recorded trace for ${actor.value} is not as long as expected."))
        .isEqualTo(expected.size)

    for ((expectedFragment, actualFragment) in expected.zip(actual)) {
        assertThat(actualFragment)
            .describedAs(makeDescriptionMessage("Trace fragments are not of the same class."))
            .hasSameClassAs(expectedFragment)

        assertThat(actualFragment.actor)
            .describedAs(makeDescriptionMessage("Trace fragments do not belong to the same actor."))
            .isEqualTo(expectedFragment.actor)

        assertThat(actualFragment.method)
            .describedAs(makeDescriptionMessage("Trace fragments do not refer to the same method."))
            .isEqualTo(expectedFragment.method)

        when (actualFragment) {
            is TraceFragment.Invocation -> {
                assertThat(futureIds)
                    .describedAs(makeDescriptionMessage("There is an error in the expected trace fragment. The future ${expectedFragment.future} has already been created."))
                    .doesNotContainKey(expectedFragment.future)

                futureIds.put(expectedFragment.future, actualFragment.future)
            }

            else ->
                assertThat(actualFragment.future)
                    .describedAs(makeDescriptionMessage("The futures of the trace fragments do not match."))
                    .isEqualTo(futureIds[expectedFragment.future])
        }
    }
}

fun assertTraces(expected: Map<Class, List<TraceFragment>>, actual: Map<Class, List<TraceFragment>>) {
    assertThat(actual.keys)
        .describedAs("Recorded traces do not cover the same actors as the expected traces.")
        .containsExactlyInAnyOrderElementsOf(expected.keys)

    for (actor in expected.keys) {
        assertTrace(actor, expected[actor]!!, actual[actor]!!)
    }
}