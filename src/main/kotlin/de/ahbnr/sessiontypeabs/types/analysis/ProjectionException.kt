package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.GlobalType
import java.lang.Exception

class ProjectionException(
    val label: GlobalType,
    message: String
): Exception(message)