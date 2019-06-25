package de.ahbnr.sessiontypeabs.codegen.astmods

import de.ahbnr.sessiontypeabs.codegen.scheduler
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.CondensedType
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
    classToType: Map<Class, CondensedType>
): ModificationLog {
    // TODO: Check if import is already present. Only import, if a class is present for which types are available
    m.addImport(StarImport("SessionTypeABS.SchedulerHelpers"))

    // We need to have an unique name for each ABS scheduler function we generate in the module.
    // Therefore we derive them from a counter.
    var schedulerNameCounter = 0

    val modLog = ModificationLog()

    for (decl in m.decls) {
        val qualifiedClass = Class(decl.qualifiedName)

        if (decl is ClassDecl && classToType.contains(qualifiedClass)) {
            val type = classToType[qualifiedClass]!!
            val automaton = genAutomaton(type)

            val schedulerName = "sched" + schedulerNameCounter++
            val scheduler = scheduler(schedulerName, automaton)

            m.addDecl(scheduler)
            modLog.createdSchedulers.add(scheduler)

            enforceAutomatonOnClass(decl, automaton, schedulerName)

            modLog.modifiedClasses.add(decl)
        }
    }

    return modLog
}

class ModificationLog(
    val modifiedClasses: MutableList<ClassDecl> = mutableListOf(),
    val createdSchedulers: MutableList<FunctionDecl> = mutableListOf()
) {
    fun add(rhs: ModificationLog) {
        modifiedClasses.addAll(rhs.modifiedClasses)
        createdSchedulers.addAll(rhs.createdSchedulers)
    }

    fun allDecls(): List<Decl> =
        modifiedClasses.plus(createdSchedulers)
}
