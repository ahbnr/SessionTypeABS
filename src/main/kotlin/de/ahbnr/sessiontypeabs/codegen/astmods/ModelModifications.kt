package de.ahbnr.sessiontypeabs.codegen.astmods

import de.ahbnr.sessiontypeabs.types.LocalType

import org.abs_models.backend.prettyprint.DefaultABSFormatter
import org.abs_models.frontend.ast.*

import java.io.PrintWriter

/**
 * This file contains functions which allow to modify an ABS model, such that it adheres to a set of Session Types
 * at runtime.
 */

/**
 * Modifies an ABS model, such that it complies to a given set of Session Types at runtime.
 *
 * Use [de.ahbnr.sessiontypeabs.types.parseFile] to parse the Session Types from files.
 *
 * @return list of modified or created declarations
 */
fun enforceSessionTypesOnModel(
    model: Model,
    classToType: Map<String, LocalType>
): List<Decl> {
    val modifiedDecls = mutableListOf<Decl>()

    for (m in model.moduleDecls) {
        modifiedDecls.addAll(enforceSessionTypesOnModule(m, classToType))
    }

    return modifiedDecls
}
