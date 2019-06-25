package de.ahbnr.sessiontypeabs.codegen

import de.ahbnr.sessiontypeabs.*
import de.ahbnr.sessiontypeabs.types.Method
import org.abs_models.frontend.ast.*

/**
 * This file provides functions for building scheduler functions.
 * In particular it provides functions to build scheduler functions which comply with a given Session Automaton.
 */

/**
 * Generates an Annotation AST node which declares a scheduler function for a class.
 *
 * Generated code:
 * [Scheduler: <shedfun>(<params>)]
 */
fun schedulerAnnotation(schedfun: String, vararg params: String) =
    TypedAnnotation(
        FnApp(
            schedfun,
            List(*(params.map{s -> VarUse(s)}.toTypedArray()))
        ),
        DataTypeUse("Scheduler", List())
    )

/**
 * Generates the function declaration AST node for a ABS scheduler function
 * (https://abs-models.org/manual/#sec:schedulers) which complies to the given [automaton].
 *
 * For details on the application of state transitions, see [stateSwitchCase].
 *
 * Example for generated code:
 * ```
 * def sched(queue, q, r0, r1) =
 *   SessionTypeABS.SchedulerHelpers.applyProtocol
 *      ((List<Process> queue) => [stateSwitchCase])
 *      (set["m1", m2"], queue)
 * ```
 */
fun scheduler(name: String, automaton: SessionAutomaton) =
    funDecl(
        maybeT(processT()),
        name,
        List(
            queueParameter(),
            stateParameter(),
            *registerParameters(automaton)
        ),
        applyProtocol(
            lambdaDecl(
                List(queueParameter()),
                stateSwitchCase(stateFieldIdentifier, automaton)
            ),
            automaton.affectedMethods().map{it.value}.toSet(),
            VarUse(schedulerFunQueueParamIdentifier)
        )
    )

/**
 * Generates an case-of AST node, which selects the next method to schedule depending on the current state, such
 * that the class this scheduling is used on complies with the given [automaton].
 *
 * This function is used as part of [scheduler] to generate a full ABS scheduler function
 * (https://abs-models.org/manual/#sec:schedulers).
 *
 * Example for generated code:
 * ```
 * case q {
 *   0 => SessionTypeABS.SchedulerHelpers.matchNamesOrRegisters(set["m1"], set(Nil), queue);
 *   1 => SessionTypeABS.SchedulerHelpers.matchNamesOrRegisters(set["m1"], set[r0], queue);
 *   _ => Nothing;
 * }
 * ```
 *
 * @param stateParam identifier of the variable which stores the current state of the automaton.
 */
private fun stateSwitchCase(stateParam: String, automaton: SessionAutomaton) =
    CaseExp(
        VarUse(stateParam),
        List(
            *(
                    automaton.Q.map{q ->
                        CaseBranch(
                            LiteralPattern(IntLiteral(q.toString())),
                            matchNamesOrRegistersForState(q, automaton)
                        )
                    } + List(
                        CaseBranch(
                            UnderscorePattern(),
                            nothingC()
                        )
                    )
                    ).toTypedArray()
        )
    )

/**
 * Generates an AST node for a call to SessionTypeABS.SchedulerHelpers.matchNamesOrRegisters with the names of those
 * methods as parameter, which can be invoked in the current [state] of the [automaton].
 * Also those registers are passed as parameters, which contain the futures of the methods which may be reactivated
 * in the current [state].
 */
private fun matchNamesOrRegistersForState(state: Int, automaton: SessionAutomaton): FnApp {
    val transitionVerbs = automaton.transitionsForState(state).map{t -> t.verb}
    val methodNames = mutableSetOf<Method>()
    val registers = mutableSetOf<Int>()

    transitionVerbs.forEach {
        when(it) {
            is TransitionVerb.InvocREv -> methodNames.add(it.method)
            is TransitionVerb.ReactEv -> registers.add(it.register)
        }
    }

    return matchNamesOrRegisters(methodNames.map{it.value}.toSet(), registers)
}

