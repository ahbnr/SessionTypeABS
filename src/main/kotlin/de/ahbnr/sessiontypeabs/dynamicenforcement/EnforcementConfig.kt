package de.ahbnr.sessiontypeabs.dynamicenforcement

import de.ahbnr.sessiontypeabs.types.Method

data class EnforcementConfig (
    // dont modify model to enforce scheduling strategy derived from session types
    val noEnforcement: Boolean = false,

    // These methods will always be permitted to be scheduled, regardless of the Session Type
    val whitelistedMethods: Set<Method> = emptySet()
)