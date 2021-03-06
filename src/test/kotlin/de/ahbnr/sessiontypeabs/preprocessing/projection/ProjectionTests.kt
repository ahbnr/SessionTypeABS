package de.ahbnr.sessiontypeabs.preprocessing.projection

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.execute
import de.ahbnr.sessiontypeabs.types.*
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType

import org.junit.jupiter.api.Test

class ProjectionTests {
    @Test
    fun `project initialization`() {
        val input = ClassLoader.getSystemResourceAsStream("globaltypes/MinimalType.st")
        val globalType = parseGlobalType(input!!, "MinimalType.st")

        val analysis = execute(CombinedAnalysis(), globalType)
        val projection = project(analysis, Class("O"))

        val expectedType =
            LocalType.Concatenation(
                LocalType.Initialization(
                    f = Future("f"),
                    m = Method("m")
                ),
                LocalType.Resolution(
                    f = Future("f"),
                    constructor = null
                )
            )


        assertEquals(expectedType, projection.type)
    }

    @Test
    fun `method reading result`() {
        val input = ClassLoader.getSystemResourceAsStream("globaltypes/modelanalysis/Branching1/Branching1.st")
        val globalType = parseGlobalType(input!!, "Branching1.st")

        val analysis = execute(CombinedAnalysis(), globalType)
        val objectProjection = project(analysis, Class("A"))
        val methodProjection =
            project(objectProjection, Class("A"), Future("f"))

        val expectedType =
            MethodLocalType.Sending(
                receiver = Class("B"),
                f = Future("f2"),
                m = Method("m1")
            ) concat
            MethodLocalType.Suspension(
                suspendedFuture = Future("f"),
                awaitedFuture = Future("f2")
            ) concat
            MethodLocalType.Fetching(
                f = Future("f2"),
                constructor = null
            ) concat
            MethodLocalType.Offer(
                f = Future("f2"),
                branches = mapOf(
                    ADTConstructor("Ok") to MethodLocalType.Sending(
                        receiver = Class("B"),
                        f = Future("f3"),
                        m = Method("m2")
                    ),
                    ADTConstructor("Error") to MethodLocalType.Sending(
                        receiver = Class("B"),
                        f = Future("f4"),
                        m = Method("m3")
                    )
                )
            ) concat
            MethodLocalType.Resolution(
                f = Future("f"),
                constructor = null
            )

        assertEquals(expectedType, methodProjection)
    }
}