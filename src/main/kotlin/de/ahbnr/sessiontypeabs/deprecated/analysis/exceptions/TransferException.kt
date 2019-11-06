package de.ahbnr.sessiontypeabs.deprecated.analysis.exceptions

import de.ahbnr.sessiontypeabs.compiler.exceptions.GlobalTypeException
import de.ahbnr.sessiontypeabs.types.GlobalType

class TransferException(
    override val type: GlobalType,
    message: String
): GlobalTypeException(type, message)