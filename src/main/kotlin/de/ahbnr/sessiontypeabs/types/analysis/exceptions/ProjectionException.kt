package de.ahbnr.sessiontypeabs.types.analysis.exceptions

import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.types.GlobalType
import java.lang.Exception

class ProjectionException(
    type: GlobalType,
    message: String
): GlobalTypeException(type, message)