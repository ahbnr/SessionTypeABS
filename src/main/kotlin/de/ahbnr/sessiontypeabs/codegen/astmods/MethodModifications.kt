package de.ahbnr.sessiontypeabs.codegen.astmods

import de.ahbnr.sessiontypeabs.codegen.analysis.ReactivationPoint
import de.ahbnr.sessiontypeabs.codegen.analysis.findReactivationPoints
import de.ahbnr.sessiontypeabs.codegen.*
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.analysis.SessionAutomaton
import de.ahbnr.sessiontypeabs.types.analysis.Transition
import de.ahbnr.sessiontypeabs.types.analysis.TransitionVerb

import org.abs_models.frontend.ast.*

import kotlin.IllegalArgumentException

/**
 * This file contains functions modifying the AST of ABS methods.
 * Foremost for the purpose of implementing Session Automata at runtime.
 */

/**
 * Adds statements to the start of the given method implementing the InvocREv events of the given
 * Session Automaton.
 *
 * For example code, please see the documentation of [genInvocREvTransitionSwitchCode].
 */
fun introduceInvocREvStateTransitions(methodImpl: MethodImpl, automaton: SessionAutomaton) {
    val invocTransitions = automaton
        .transitionsForMethod(Method(methodImpl.methodSigNoTransform.name))
        .filter{t -> t.verb is TransitionVerb.InvocREv }
        .toSet()

    val stateModStmt = genInvocREvTransitionSwitchCode(invocTransitions)

    methodImpl.blockNoTransform.prependStmt(stateModStmt)
}

/**
 * Searches an ABS method for all points where it could possibly be reactivated, it even considers other
 * synchronously called methods (see [findReactivationPoints]).
 * It then inserts the proper state transition statements at those points for all ReactEv transitions in the
 * [automaton].
 *
 * For example code, please see the documentation of [genReactivationStateTransition] as well as
 * [genAwaitReplacementForReactivation], [replaceAwaitAsyncCallForReactivation] and
 * [genSuspendReplacementForReactivation].
 */
fun introduceReactivationTransitions(methodImpl: MethodImpl, context: ClassDecl, automaton: SessionAutomaton) {
    val methodName = Method(methodImpl.methodSigNoTransform.name)

    // Gather all reactivation statements concerning the method and build the
    // corresponding state transition ASTs
    val transitionStmts = automaton
        .transitionsForMethod(methodName)
        .filter{t -> t.verb is TransitionVerb.ReactEv }
        .map{t -> genReactivationStateTransition(t) }
        .toTypedArray()

    if (transitionStmts.isNotEmpty()) {
        // Since we may need to construct additional variables, we need a continuous
        // counter to avoid duplicate variable names.
        var variableNameCount = 0

        findReactivationPoints(methodImpl, context, mutableSetOf()).forEach {
            when (it) {
                is ReactivationPoint.Await -> it.awaitStmt.replaceWith( // TODO: Achtung: AwaitStmt und EffExp mixbar?
                    genAwaitReplacementForReactivation(it.awaitStmt, transitionStmts)
                )
                is ReactivationPoint.AwaitAsyncCall ->
                    replaceAwaitAsyncCallForReactivation(
                        it.awaitAsyncCall,
                        transitionStmts,
                        variableNameCount++
                    )
                is ReactivationPoint.Suspend -> it.suspendStmt.replaceWith(
                    genSuspendReplacementForReactivation(it.suspendStmt, transitionStmts)
                )
            }
        }
    }
}

/**********************************************************************
 * Code generator function which do not modify existing ASTs themselves
 **********************************************************************/

/**
 * Generates the ABS code necessary to achieve a state transition for a InvocREv event.
 *
 * Example of generated code:
 *
 * ```
 * r0 = thisDestiny;
 * q = 2;
 * ```
 *
 * @throws IllegalArgumentException if the [transition.verb] is not of InvocREv type
 */
private fun genInvocREvTransitionCode(transition: Transition): Block {
    val stateMod = AssignStmt(
        List(), //Annotations
        FieldUse(stateFieldIdentifier),
        IntLiteral(transition.q2.toString())
    )

    val verb = transition.verb as? TransitionVerb.InvocREv
        ?:throw IllegalArgumentException("Transition parameter is not an InvocREv transition")

    val registerMod =
        AssignStmt(
            List(), //Annotations
            FieldUse(registerIdentifier(verb.register)),
            justC(ThisDestinyExp())
        )

    return Block(
        List(), // Annotations
        List(
            *listOf(
                registerMod,
                stateMod
            ).toTypedArray()
        )
    )
}


/**
 * Generates the ABS code which chooses and applies the right InvocREv state transition after calling a method.
 * It should be placed at the beginning of a method, see [introduceInvocREvStateTransitions].
 *
 * Example of generated code:
 * ```
 * case q {
 *   0 => {
 *      r0 = thisDestiny;
 *      q = 1;
 *   }
 *   1 => {
 *      r1 = thisDestiny;
 *      q = 2;
 *   }
 *   _ => {
 *      assert(false);
 *   }
 * }
 * ```
 *
 * @throws IllegalArgumentException if one of the [transitions] is not of InvocREv type
 */
private fun genInvocREvTransitionSwitchCode(transitions: Set<Transition>) =
    CaseStmt(
        List(), // Annotations
        VarUse(stateFieldIdentifier),
        List(
            *(
                    transitions.map{t ->
                        CaseBranchStmt(
                            LiteralPattern(IntLiteral(t.q1.toString())),
                            genInvocREvTransitionCode(t)
                        )
                    } + List(
                        CaseBranchStmt(
                            UnderscorePattern(),
                            Block(
                                List(), // Annotations
                                List(
                                    AssertStmt(
                                        List(), // Annotations
                                        falseC()
                                    )
                                )
                            )
                        )
                    )
                    ).toTypedArray()
        )
    )

/**
 * Generates ABS code which implements the necessary state transitions of a Session Automaton upon a
 * method reactivation.
 *
 * The generated code is to be placed after `await` statements etc., see
 * [de.ahbnr.sessiontypeabs.analysis.ReactivationPoint]
 *
 * Example of generated code_
 *
 * ```
 * if (q == 1 && Just(thisDestiny) == r0) {
 *   q = 2;
 * }
 * ```
 *
 * @throws IllegalArgumentException if [t.verb] is not of InvocREv type
 */
private fun genReactivationStateTransition(t: Transition): Stmt {
    if (t.verb !is TransitionVerb.ReactEv) {
        throw IllegalArgumentException("Transition parameter is not an InvocREv transition")
    }

    return ifThen(
        AndBoolExp(
            EqExp(FieldUse(stateFieldIdentifier), IntLiteral(t.q1.toString())),
            EqExp(
                justC(ThisDestinyExp()),
                FieldUse(registerIdentifier(t.verb.register))
            )
        ),
        AssignStmt(
            List(), // no annotations
            FieldUse(stateFieldIdentifier),
            IntLiteral(t.q2.toString())
        )
    )
        /* Result:
          if (q == X && Just(thisDestiny) == rI) { // TODO: Aussagekräftig genug? Werden Register je geleert? Vielleicht relevant nicht im Protokoll genannte Funktionen auch aufgerufen werden können
            q = Y;
          }
        */
}

/**
 * Generates ABS code which is meant to replace await statements, such that the proper state
 * transitions for reactivation are applied, such that the method complies with a SessionAutomaton.
 *
 * Example result:
 *
 * Original:
 * ```
 * await f?;
 * ```
 *
 * Replacement:
 * ```
 * {
 *   await f?;
 *   <state transitions>
 * }
 * ```
 *
 * @param transitionStmts these should be generated by [genReactivationStateTransition]
 */
private fun genAwaitReplacementForReactivation(awaitStmt: AwaitStmt, transitionStmts: Array<Stmt>) =
    Block(
        List(), //no annotations
        List(
            awaitStmt.treeCopyNoTransform(),
            *transitionStmts
        )
    )

/**
 * Generates ABS code which is meant to replace suspend statements, such that the proper state
 * transitions for reactivation are applied, such that the method complies with a SessionAutomaton.
 *
 * Example result:
 *
 * Original:
 * ```
 * suspend;
 * ```
 *
 * Replacement:
 * ```
 * {
 *   suspend;
 *   <state transitions>
 * }
 * ```
 *
 * @param transitionStmts these should be generated by [genReactivationStateTransition]
 */
private fun genSuspendReplacementForReactivation(suspendStmt: SuspendStmt, transitionStmts: Array<Stmt>) =
    Block(
        List(), //Annotations
        List(
            suspendStmt.treeCopyNoTransform(),
            *transitionStmts
        )
    )

/**
 * Replaces AwaitAsyncCall expressions within their surrounding AST, such that the proper state
 * transitions for reactivation are applied, such that the method complies with a SessionAutomaton.
 *
 * TODO: Handle Unit return type!
 *
 * Example result:
 *
 * Original:
 * ```
 * if (await o!m()) {
 *   ...
 * }
 * ```
 *
 * Replacement:
 * ```
 * {
 *   Bool awaitCallCache0 = await o!m()M
 *   <state transitions>
 *   if (awaitCallCache0) {
 *     ...
 *   }
 * }
 * ```
 *
 * @param transitionStmts these should be generated by [genReactivationStateTransition]
 * @param variableNameCount this function introduces new variables. The variable name is generated from this integer,
 *                          which should be unique for the surrounding method.
 */
private fun replaceAwaitAsyncCallForReactivation(awaitAsyncCall: AwaitAsyncCall, transitionStmts: Array<Stmt>, variableNameCount: Int) {
    val varName = "awaitCallCache$variableNameCount"
    val expTypeName = awaitAsyncCall.type.decl.qualifiedName
    val stmtParent = awaitAsyncCall.closestParent(Stmt::class.java)
    val expDeepCopy = awaitAsyncCall.treeCopyNoTransform()

    // First we replace the call expression with a new variable
    awaitAsyncCall.replaceWith(
        VarUse(varName)
    )

    if (stmtParent == null) {
        throw IllegalArgumentException("Can not replace the AwaitAsyncCall expression, since it is not used within any statement. Not even an ExpressionStmt.")
    }

    // Then we initialize the new variable with the expression value before the statement the expression was used in
    // before.
    stmtParent.replaceWith(
        Block(
            List(), //Annotations
            List(
                VarDeclStmt(
                    List(), // no annotations
                    VarDecl(
                        varName,
                        UnresolvedTypeUse(
                            expTypeName,
                            List() // no annotations
                        ),
                        Opt(expDeepCopy)
                    )
                ),
                *transitionStmts,
                stmtParent.treeCopyNoTransform()
            )
        )
    )
}
