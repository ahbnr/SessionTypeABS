package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions

import de.ahbnr.sessiontypeabs.types.GlobalType

class MergeException(
    override val type: GlobalType,
    message: String
): ConfigurableAnalysisException(type, message)