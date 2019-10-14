package de.ahbnr.sessiontypeabs.reordering

import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError

class ConsecutiveCalls {
    private val fileHead = "ConsecutiveCalls"
    private val fileDir = "consecutive_calls"
    private val expectedTraces = mapOf(
            Class("Caller") to
                listOf(
                    TraceFragment.Invocation(
                        actor = Class("Caller"),
                        method = Method("main"),
                        future = Future("mainTask")
                    )
                ),
            Class("Callee") to
                (1..50).map {
                    listOf(
                        TraceFragment.Invocation(
                            actor = Class("Callee"),
                            method = Method("a"),
                            future = Future("aTask$it")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Callee"),
                            method = Method("b"),
                            future = Future("bTask$it")
                        )
                    )
                }.flatten()
        )

    @Test
    fun `many consecutive protocol executions must be in right order when using a session type`() {
        performReorderingTest(fileHead = fileHead, fileDir=fileDir, noEnforcement = false, expectedTraces = expectedTraces)
    }

    @Test
    fun `many consecutive calls are very likely in the wrong order, when not using a session type`() {
        assertThrows<AssertionFailedError> {
            performReorderingTest(fileHead = fileHead, fileDir=fileDir, noEnforcement = true, expectedTraces = expectedTraces)
        }
    }
}
