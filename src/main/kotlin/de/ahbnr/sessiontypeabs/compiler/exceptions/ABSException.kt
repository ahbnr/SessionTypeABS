package de.ahbnr.sessiontypeabs.compiler.exceptions

import org.abs_models.common.CompilerCondition

open class ABSException(
    val errors: List<CompilerCondition>,
    message: String
): CompilerException(message)
