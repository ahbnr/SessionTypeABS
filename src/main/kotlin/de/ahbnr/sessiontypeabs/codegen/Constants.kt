package de.ahbnr.sessiontypeabs.codegen

import de.ahbnr.sessiontypeabs.SessionAutomaton;

/**
 * This file contains definitions of constant strings etc. used in this package.
 */

/*******************************************************************
 * Identifiers for variables and fields used in the generated code:
 *******************************************************************/

const val stateFieldIdentifier = "q"

const val schedulerFunQueueParamIdentifier = "queue"

/**
 * Determines the identifier string of a register within an ABS model
 */
fun registerIdentifier(register: Int) = "r$register"

fun SessionAutomaton.registerIdentifiers() = registers.map(::registerIdentifier)
