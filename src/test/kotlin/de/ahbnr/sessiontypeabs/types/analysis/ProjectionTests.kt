package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.Method

import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat

class ProjectionTests {
    @Test
    fun `project initialization`() {
        val input = ClassLoader.getSystemResourceAsStream("globaltypes/MinimalType.st")
        val globalType = parseGlobalType(input!!, "MinimalType.st")

        val analysis = execute(CombinedDomain(), globalType)
        val projection = project(analysis, Class("O"))

        val expectedType = LocalType.Initialization(
            f = Future("f"),
            m = Method("m")
        )

        assertThat(projection)
            .usingRecursiveComparison()
            .ignoringAllOverriddenEquals()
            .withStrictTypeChecking()
            .isEqualTo(expectedType)
    }
}