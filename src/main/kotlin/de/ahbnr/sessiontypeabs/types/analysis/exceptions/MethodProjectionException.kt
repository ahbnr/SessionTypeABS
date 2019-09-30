package de.ahbnr.sessiontypeabs.types.analysis.exceptions

import de.ahbnr.sessiontypeabs.compiler.exceptions.LocalTypeException
import de.ahbnr.sessiontypeabs.types.LocalType

class MethodProjectionException(
    type: LocalType,
    message: String
): LocalTypeException(type, message)