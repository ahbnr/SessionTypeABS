package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions

import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.types.GlobalType
import java.lang.Exception

abstract class ConfigurableAnalysisException(
    override val type: GlobalType,
    message: String
): GlobalTypeException(type, message)