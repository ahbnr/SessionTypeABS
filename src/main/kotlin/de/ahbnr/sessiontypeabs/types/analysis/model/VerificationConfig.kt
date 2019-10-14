package de.ahbnr.sessiontypeabs.types.analysis.model

data class VerificationConfig (
    val noChecks: Boolean = false, // perform no static verification
    val strictMain: Boolean = true, // If true, no calls except for the initial call are allowed in Main.
                                    // Useful to deactivate to call tracing functions for debugging.
    val shareableInterfaces: Set<String> = emptySet() // Interfaces which are allowed to be shared between
                                                      // actors. Useful for debugging.
)