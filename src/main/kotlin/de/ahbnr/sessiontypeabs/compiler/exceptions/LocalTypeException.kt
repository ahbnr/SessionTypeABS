package de.ahbnr.sessiontypeabs.compiler.exceptions

import de.ahbnr.sessiontypeabs.types.LocalType

open class LocalTypeException(
    open val type: LocalType,
    message: String
): CompilerException(message)
