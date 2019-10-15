package de.ahbnr.sessiontypeabs.codegen.astmods

import de.ahbnr.sessiontypeabs.codegen.intT
import de.ahbnr.sessiontypeabs.codegen.nothingC
import de.ahbnr.sessiontypeabs.codegen.registerT
import de.ahbnr.sessiontypeabs.codegen.schedulerAnnotation
import de.ahbnr.sessiontypeabs.codegen.*
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.dynamicenforcement.automata.SessionAutomaton

import org.abs_models.frontend.ast.*

/**
 * This file contains functions which allow to modify an ABS class declaration, such that it adheres to a Session Automaton at
 * runtime
 */

/**
 * Modifies an ABS class, such that it complies to a given SessionAutomaton at runtime.
 *
 * @param schedulerName the surrounding ABS module must provide a ABS scheduler function which implements the Session
 *                      Automaton. This is the name of that function.
 *                      See also [de.ahbnr.sessiontypeabs.codegen.scheduler].
 */
fun enforceAutomatonOnClass(classDecl: ClassDecl, automaton: SessionAutomaton, schedulerName: String) {
    // TODO check whether the order of these modifications matters
    introduceStateField(classDecl, automaton)
    introduceRegisters(classDecl, automaton)
    introduceInvocREvStateTransitions(classDecl, automaton)
    introduceSchedulerAnnotation(classDecl, schedulerName, automaton)
    introduceReactivationTransitions(classDecl, automaton)
    introducePostConditions(classDecl, automaton)  // must be called last
}

/**********************************************************************************************************
 * Functions for modifying aspects of a class necessary for enforcing a Session Automaton on it at runtime.
 **********************************************************************************************************/
/**
 * Places a field in a class declaration which keeps track of the current state of the automaton
 *
 * Example of the effect on the ABS model:
 *
 * ```
 * class C ... {
 *   ...
 *   int q = 0;
 *   ...
 * }
 * ```
 */
fun introduceStateField(clazz: ClassDecl, automaton: SessionAutomaton) =
    clazz.addField(
        FieldDecl(
            stateFieldIdentifier,
            intT(),
            Opt(
                IntLiteral(automaton.q0.toString())
            ),
            List(), // Annotations
            false // ABS.ast notes: "For Component Model".
        )
    )

/**
 * Places fields in a class declaration which can be used as registers for a Session Automaton.
 * (They are meant to store futures as soon as they are available)
 *
 * Example of the effect on the ABS model:
 *
 * ```
 * class C ... {
 *   ...
 *   Register r0 = None;
 *   Register r1 = None;
 *   ...
 * }
 * ```
 */
fun introduceRegisters(clazz: ClassDecl, automaton: SessionAutomaton) {
    automaton.registers.forEach {
        clazz.addField(
            FieldDecl(
                registerIdentifier(it),
                registerT(),
                Opt(
                    nothingC()
                ),
                List(), // Annotations
                false // ABS.ast notes: "For Component Model".
            )
        )
    }
}

/**
 * Adds an scheduler annotation to a class declaration.
 *
 * Example of the effect on the ABS model:
 *
 * ```
 * [Scheduler: sched(queue, q, r0, r1), ...]
 * class C ... {
 *   ...
 * }
 * ```
 */
fun introduceSchedulerAnnotation(
    clazz: ClassDecl,
    schedfun: String,
    automaton: SessionAutomaton
) =
    clazz.addAnnotation(
        schedulerAnnotation(
            schedfun,
            schedulerFunQueueParamIdentifier,
            stateFieldIdentifier,
            *automaton.registerIdentifiers().toTypedArray()
        )
    )

/**
 * Adds state transition code to the beginning of all methods of the given class for which the given automaton
 * contains InvocREv transitions.
 *
 * For an example of the introduced code per method, see the documentation of [genInvocREvTransitionSwitchCode].
 */
fun introduceInvocREvStateTransitions(clazz: ClassDecl, automaton: SessionAutomaton) {
    val affectedMethods = automaton.affectedMethods()

    //INFO: Use lookupMethod() instead?
    clazz.methodsNoTransform.forEach {
        if (affectedMethods.contains(Method(it.methodSigNoTransform.name))) {
            introduceInvocREvStateTransitions(it, automaton)
        }
    }
}

/**
 * Adds code for checking post-conditions using assertions to all methods of the given class for which the given automaton
 * contains transitions with such post-conditions.
 *
 * ATTENTION: Since this function needs to insert code at the very beginning of methods,
 * it should always be the last one in a series of modifications.
 *
 * For an example of the introduced code per method, see the documentation of
 * [de.ahbnr.sessiontypeabs.codegen.astmods.MethodModificationsKt.introducePostConditions].
 */
fun introducePostConditions(clazz: ClassDecl, automaton: SessionAutomaton) {
    val affectedMethods = automaton.affectedMethods()

    //INFO: Use lookupMethod() instead?
    clazz.methodsNoTransform.forEach {
        if (affectedMethods.contains(Method(it.methodSigNoTransform.name))) {
            introducePostConditions(it, automaton)
        }
    }
}

/**
 * Adds state transition code to all methods of the given class for which the given automaton
 * contains reactivation transitions.
 *
 * For an example of the introduced code per method, see the documentation of
 * [de.ahbnr.sessiontypeabs.codegen.astmods.MethodModificationsKt.introduceReactivationTransitions].
 */
fun introduceReactivationTransitions(classDecl: ClassDecl, automaton: SessionAutomaton) {
    val affectedMethods = automaton.affectedMethods()

    classDecl
        .methodsNoTransform
        .filter{m -> affectedMethods.contains(Method(m.methodSigNoTransform.name))}
        .forEach {
            introduceReactivationTransitions(it, classDecl, automaton)
        }
}

// ERLANG: #process_info{destiny=Fut} = get(process_info),
// TODO: ContractInference:_ src/main/java/deadlock/analyser/inference/ContractInference.java
// CallResolver?: src/main/java/org/abs_models/frontend/delta/OriginalCallResolver.jadd
