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
 */
fun enforceSessionTypesOnModel(
    model: Model,
    classToType: Map<String, LocalType>,
    printer: PrintWriter,
    formatter: DefaultABSFormatter
) {

    for (m in model.moduleDecls) {
        enforceSessionTypesOnModule(m, classToType, printer, formatter)
    }
}
