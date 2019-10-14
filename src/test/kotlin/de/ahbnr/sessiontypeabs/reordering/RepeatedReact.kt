package de.ahbnr.sessiontypeabs.reordering

import de.ahbnr.sessiontypeabs.generator.replicate
import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import java.lang.AssertionError

class RepeatedReact {
    private val fileHead = "RepeatedReact"
    private val fileDir = "repeated_react"
    private val rounds = 50
    private val expectedTraces = mapOf(
            Class("Caller") to
                listOf(
                    TraceFragment.Invocation(
                        actor = Class("Caller"),
                        method = Method("main"),
                        future = Future("mainTask")
                    )
                ) +
                (1..rounds).map{
                    listOf(
                        TraceFragment.Invocation(
                            actor = Class("Caller"),
                            method = Method("callback1"),
                            future = Future("callbackTask1_$it")
                        ),
                        TraceFragment.Invocation(
                            actor = Class("Caller"),
                            method = Method("callback2"),
                            future = Future("callbackTask2_$it")
                        ),
                        TraceFragment.Reactivation(
                            actor = Class("Caller"),
                            method = Method("main"),
                            future = Future("mainTask")
                        )
                    )
                }.flatten(),
            Class("Callee") to
                (1..rounds).map {
                    listOf(
                        TraceFragment.Invocation(
                            actor = Class("Callee"),
                            method = Method("a"),
                            future = Future("aTask$it")
                        )
                    )
                }.flatten()
        )
    private val whitelisted = setOf(Method("setCaller"))

    @Test
    fun `many consecutive calls must be in right order when using a session type`() {
        performReorderingTest(fileHead = fileHead, fileDir=fileDir, noEnforcement = false, expectedTraces = expectedTraces, whitelisted = whitelisted)
    }

    @Test
    fun `many consecutive calls are very likely in the wrong order, when not using a session type`() {
        assertThrows<AssertionError> {
            performReorderingTest(fileHead = fileHead, fileDir=fileDir, noEnforcement = true, expectedTraces = expectedTraces, whitelisted = whitelisted)
        }
    }
}
