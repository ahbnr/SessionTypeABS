package de.ahbnr.sessiontypeabs.tracing

import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future

sealed class TraceFragment {
    abstract val actor: Class
    abstract val method: Method
    abstract val future: Future

    data class Invocation(
        override val actor: Class,
        override val method: Method,
        override val future: Future
    ) : TraceFragment()

    data class Reactivation(
        override val actor: Class,
        override val method: Method,
        override val future: Future
    ) : TraceFragment()
}
