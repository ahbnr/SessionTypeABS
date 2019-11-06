package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.loadType

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserErrorTests {
    @ParameterizedTest
    @MethodSource("typeProvider")
    fun `parse types`(input: String) {
        assertThrows<ParserException> {
            loadType(input)
        }
    }

    fun typeProvider() = Stream.of(
        Arguments.of(
            ""
        ),
        Arguments.of(
            "P -f-> Q:m"
        ),
        Arguments.of(
            "1 -f-> P:m"
        ),
        Arguments.of(
            """
                0 -f-> P:m
                P resolves f
            """.trimIndent()
        ),
        Arguments.of(
            """
                0 -f-> P:m.
                P{
                    skip
                }*.
                P resolves f
            """.trimIndent()
        )
    )
}