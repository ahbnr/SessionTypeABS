package de.ahbnr.sessiontypeabs.types

import org.assertj.core.api.Assertions.assertThat

fun assertEquals(expected: MethodLocalType?, actual: MethodLocalType?, message: String = "Expected Method Local Type $expected to be equal to $actual, but they are not.") {
    if (expected == null || actual == null) {
        assertThat(actual)
            .describedAs(message)
            .isEqualTo(expected)
    }

    else {
        assertThat(actual.head)
            .describedAs(message)
            .isEqualTo(expected.head)

        assertEquals(expected.tail, actual.tail, message)
    }
}

fun assertEquals(expected: LocalType?, actual: LocalType?, message: String = "Expected Object Local Type $expected to be equal to $actual, but they are not.") {
    if (expected == null || actual == null) {
        assertThat(actual)
            .describedAs(message)
            .isEqualTo(expected)
    }

    else {
        assertThat(actual.head)
            .describedAs(message)
            .isEqualTo(expected.head)

        assertEquals(expected.tail, actual.tail, message)
    }
}
