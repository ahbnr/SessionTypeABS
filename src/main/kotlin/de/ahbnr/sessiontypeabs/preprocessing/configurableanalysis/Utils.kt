package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Method

fun throwNotAtomic(context: GlobalType): Nothing =
    when (context) {
        is GlobalType.Branching,
        is GlobalType.Repetition,
        is GlobalType.Concatenation ->
            throw RuntimeException(
                """
                    Configurable Analysis of a Session Type failed, since a analysis function got passed a non-atomic
                    session type.
                    This should never happen and is a programmer error.
                """.trimIndent()
            )
        else -> throw RuntimeException(
            """
                Configurable Analysis of a Session Type failed, since a analysis function did not handle an atomic
                session-type even though it should have.
                This should never happen and is a programmer error.
            """.trimIndent()
        )
    }

data class MethodBinding(
    val actor: Class,
    val method: Method
)