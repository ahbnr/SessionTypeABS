package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.types.Method

data class CompilerConfig (
    // Tell ABS Erlang compiler to be noisy
    val verboseErlangCompilation: Boolean = false
)