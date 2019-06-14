package de.ahbnr.sessiontypeabs.codegen.astmods

import de.ahbnr.sessiontypeabs.codegen.scheduler
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.genAutomaton

import org.abs_models.frontend.ast.*

/**
 * This file contains functions which allow to modify an ABS module, such that it adheres to a set of Session Types
 * at runtime.
 */

/**
 * Modifies an ABS module, such that it complies to a given set of Session Types at runtime.
 *
 * Use [de.ahbnr.sessiontypeabs.types.parseFile] to parse the Session Types from files.
 *
 * @return modified classes and generated schedulers
 */
fun enforceSessionTypesOnModule(
    m: ModuleDecl,
    classToType: Map<String, LocalType>
): List<Decl> {
    // TODO: Check if import is already present. Only import, if a class is present for which types are available
    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

    // We need to have an unique name for each ABS scheduler function we generate in the module.
    // Therefore we derive them from a counter.
    var schedulerNameCounter = 0

    val modifiedDecls = mutableListOf<Decl>()

    for (decl in m.decls) {
        if (decl is ClassDecl && classToType.contains(decl.qualifiedName)) {
            val type = classToType[decl.qualifiedName]!!
            val automaton = genAutomaton(type)

            val schedulerName = "sched" + schedulerNameCounter++
            val scheduler = scheduler(schedulerName, automaton)

            m.addDecl(scheduler)
            modifiedDecls.add(scheduler)

            enforceAutomatonOnClass(decl, automaton, schedulerName)

            modifiedDecls.add(decl)
        }
    }

    return modifiedDecls
}
