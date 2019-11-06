package de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.astmods

import de.ahbnr.sessiontypeabs.dynamicenforcement.EnforcementConfig
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.CondensedType

import org.abs_models.frontend.ast.*

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
    classToType: Map<Class, CondensedType>,
    enforcementConfig: EnforcementConfig = EnforcementConfig()
): ModificationLog {
    val modLog = ModificationLog()

    for (m in model.moduleDecls) {
        modLog.add(enforceSessionTypesOnModule(m, classToType, enforcementConfig))
    }

    return modLog
}
