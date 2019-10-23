package de.ahbnr.sessiontypeabs.dynamicenforcement

import de.ahbnr.sessiontypeabs.types.Method

data class EnforcementConfig (
    // dont modify model to enforce scheduling strategy derived from session types
    val noEnforcement: Boolean = false,

    // These methods will always be permitted to be scheduled, regardless of the Session Type
    val whitelistedMethods: Set<Method> = emptySet(),

    // If set, the scheduler prints a message whenever it is called.
    // Must be qualified class names.
    // Useful for statistics
    val logSchedulerCalls: Set<String> = emptySet(),

    // If set, the scheduler prints a message whenever no activation is viable for the given classes.
    // Must be qualified class names.
    // Useful for statistics on misses
    val logActivationDelay: Set<String> = emptySet()
)