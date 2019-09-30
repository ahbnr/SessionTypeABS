package de.ahbnr.sessiontypeabs.types.analysis.exceptions

import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.types.GlobalType

class FinalizationException(
    override val type: GlobalType,
    message: String
): GlobalTypeException(type, message)