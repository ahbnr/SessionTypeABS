package de.ahbnr.sessiontypeabs.compiler.exceptions

import de.ahbnr.sessiontypeabs.types.GlobalType

open class GlobalTypeException(
    open val type: GlobalType,
    message: String
): CompilerException(message)
