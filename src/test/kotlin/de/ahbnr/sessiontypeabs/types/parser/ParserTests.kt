package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method

import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ParserTests {
    @ParameterizedTest
    @MethodSource("fileAndExpectedTypeProvider")
    fun `parse types`(fileName: String, expectedType: GlobalType) {
        val input = ClassLoader.getSystemResourceAsStream("globaltypes/$fileName")
        val parsedType = parseGlobalType(input!!, fileName)

        assertThat(parsedType)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*fileContext")
            .ignoringAllOverriddenEquals()
            .withStrictTypeChecking()
            .isEqualTo(
                expectedType
            )
    }

    fun fileAndExpectedTypeProvider() = Stream.of(
        Arguments.of(
            "MinimalType.st",
            GlobalType.Concatenation(
                GlobalType.Initialization(
                    c = Class("O"),
                    f = Future("f"),
                    m = Method("m")
                ),
                GlobalType.Resolution(
                    c = Class("O"),
                    f = Future("f"),
                    constructor = null
                )
            )
        ),
        Arguments.of(
            "InteractionType.st",
            GlobalType.Concatenation(
                GlobalType.Initialization(
                    c = Class("O"),
                    f = Future("f"),
                    m = Method("m")
                ),
                GlobalType.Interaction(
                    caller = Class("O"),
                    callee = Class("P"),
                    f = Future("f2"),
                    m = Method("m")
                )
            )
        ),
        Arguments.of(
            "BranchingType.st",
            GlobalType.Concatenation(
                GlobalType.Initialization(
                    c = Class("O"),
                    f = Future("f"),
                    m = Method("m")
                ),
                GlobalType.Branching(
                    choosingActor = Class("O"),
                    branches = listOf(
                        GlobalType.Concatenation(
                            GlobalType.Interaction(
                                caller = Class("O"),
                                callee = Class("P"),
                                f = Future("f2"),
                                m = Method("a")
                            ),
                            GlobalType.Interaction(
                                caller = Class("O"),
                                callee = Class("P"),
                                f = Future("f3"),
                                m = Method("b")
                            )
                        ),
                        GlobalType.Concatenation(
                            GlobalType.Interaction(
                                caller = Class("O"),
                                callee = Class("P"),
                                f = Future("f3"),
                                m = Method("b")
                            ),
                            GlobalType.Interaction(
                                caller = Class("O"),
                                callee = Class("P"),
                                f = Future("f2"),
                                m = Method("a")
                            )
                        )
                    )
                )
            )
        )
    )
}