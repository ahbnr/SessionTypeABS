package de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.astmods

import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.analysis.ReactivationPoint
import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.analysis.findReactivationPoints
import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.*
import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.analysis.findReturnStmt
import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.tail
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.dynamicenforcement.automata.SessionAutomaton
import de.ahbnr.sessiontypeabs.dynamicenforcement.automata.Transition
import de.ahbnr.sessiontypeabs.dynamicenforcement.automata.TransitionVerb

import org.abs_models.frontend.ast.*

import kotlin.IllegalArgumentException

// TODO produce more obscure variable names to avoid name clashes

/**
 * This file contains functions modifying the AST of ABS methods.
 * Foremost for the purpose of implementing Session Automata at runtime, but
 * also to insert assertions for post-condition checking.
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
    val maybeTransitionStmts = automaton
        .transitionsForMethod(methodName)
        .filter{t -> t.verb is TransitionVerb.ReactEv } // only consider ReactEv transitions
        .map{t -> {elseCase: Stmt? -> genReactivationStateTransition(t, elseCase)} } // map them to functions which compute the state transition statements, given an else case
        .foldRight(null as Stmt?, {next, acc -> next(acc)}) // set the last else case to null and then apply these functions to each other in series, resulting in a big if-else statement

    if (maybeTransitionStmts != null) {
        // Since we may need to construct additional variables, we need a continuous
        // counter to avoid duplicate variable names.
        var variableNameCount = 0

        findReactivationPoints(methodImpl, context, mutableSetOf()).forEach {
            when (it) {
                is ReactivationPoint.Await -> it.awaitStmt.replaceWith( // TODO: Achtung: AwaitStmt und EffExp mixbar?
                    genAwaitReplacementForReactivation(it.awaitStmt, maybeTransitionStmts)
                )
                is ReactivationPoint.AwaitAsyncCall ->
                    replaceAwaitAsyncCallForReactivation(
                        it.awaitAsyncCall,
                        maybeTransitionStmts,
                        variableNameCount++
                    )
                is ReactivationPoint.Suspend -> it.suspendStmt.replaceWith(
                    genSuspendReplacementForReactivation(it.suspendStmt, maybeTransitionStmts)
                )
            }
        }
    }
}

/**
 * Adds statements to the given method implementing checks for post-conditions of the behavior defined by
 * the given Session Automaton.
 *
 * For example code, please see the documentation of [introducePostConditionAssertionsForInvocREvs].
 */
fun introducePostConditions(methodImpl: MethodImpl, automaton: SessionAutomaton) {
    // For now, only InvocREvs have post-conditions, so lets extract the relevant transitions and insert the checks
    val invocTransitions = automaton
        .transitionsForMethod(Method(methodImpl.methodSigNoTransform.name))
        .filter{t -> t.verb is TransitionVerb.InvocREv && t.verb.postCondition != null }
        .toSet()

    introducePostConditionAssertionsForInvocREvs(methodImpl, invocTransitions)
}

/**
 * Searches an ABS method for all points where it could possibly conclude its execution.
 * It then inserts assertions at those points to check for the post-conditions of the given invocREv transitions.
 *
 * ATTENTION: Since this method needs to insert code at the very beginning of the method, it should be
 * called last in a series of modifications.
 *
 * TODO: Consider exceptions as exit points.
 *   Idea: Wrap entire method in try-catch-finally and apply assertion in finally.
 *
 * Example:
 *
 * Before application of this method:
 *
 * ```
 * Int getAnswer() {
 *   return 42;
 * }
 * ```
 *
 * After application of this method:
 *
 * ```
 * Int getAnswer() {
 *   Int invocState = this.q;
 *
 *   {
 *      Int result = 42;
 *
 *      case invocState {
 *          0 => {
 *              assert(this.x != null);
 *          }
 *
 *          _ => {skip;}
 *      }
 *
 *      return result;
 *   }
 * }
 * ```
 *
 * This function uses a lot of helper functions:
 *
 * [genStateCache] is used to store the automaton state upon the initial activation of the method.
 *                 This is important to decide later one, which post-condition should be checked upon exiting.
 * [genReturnStmtPostConditionAssertReplacement] is used to generate an AST sub-tree to replace return statements, which will
 *                 apply the post-condition checks
 * [genPostConditionAssertionCases] is used to generate case statements which decide, which post-condition shall be checked.
 * [genAssertionStmtFromTransition] is used to generate "assert" statment AST nodes
 *
 * @param methodImpl method for which post-conditions shall be checked
 * @param invocREvTransitions invocREv transitions for this method with post-conditions
 * @throws IllegalArgumentException if [invocREvTransitions] contains transitions which are either no invocREv transitions or which don't have a post-condition
 */
fun introducePostConditionAssertionsForInvocREvs(methodImpl: MethodImpl, invocREvTransitions: Set<Transition>) {
    // Store state during invocation in variable, s. t. it can be checked when exiting the method
    val invocStateVarName = "invocState"

    methodImpl.blockNoTransform.prependStmt(
        genStateCache(invocStateVarName)
    )

    // Besides exceptions, methods can only exit at their syntactic end or with a return statement.
    // Since return statements can only be placed at the end of a method, either we find a return statement
    // and replace that, or we insert an assertion at the end of the method otherwise.
    val maybeReturnStmt = findReturnStmt(methodImpl.blockNoTransform)

    when (maybeReturnStmt) {
        null -> {
            methodImpl.blockNoTransform.addStmtNoTransform(
                genPostConditionAssertionCases(invocStateVarName, invocREvTransitions)
            )
        }
        else -> {
            var variableNameCount = 0

            genReturnStmtPostConditionAssertReplacement(
                maybeReturnStmt,
                invocStateVarName,
                invocREvTransitions,
                variableNameCount++ // FIXME increment never used
            )
                .apply(methodImpl.blockNoTransform)
        }
    }
}

/**********************************************************************
 * Code generator functions which do not modify existing ASTs themselves
 **********************************************************************/

/**
 * Generates the ABS code necessary to achieve a state transition for a InvocREv event.
 *
 * Example of generated code:
 *
 * ```
 * r0 = destiny;
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
            justC(DestinyExp())
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
 *      r0 = destiny;
 *      q = 1;
 *   }
 *   1 => {
 *      r1 = destiny;
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
 * if (q == 1 && Just(destiny) == r0) {
 *   q = 2;
 * }
 *
 * else {
 *   [elseCase]
 * }
 * ```
 *
 * @throws IllegalArgumentException if [t.verb] is not of InvocREv type
 */
private fun genReactivationStateTransition(t: Transition, elseCase: Stmt? = null): Stmt {
    if (t.verb !is TransitionVerb.ReactEv) {
        throw IllegalArgumentException("Transition parameter is not an InvocREv transition")
    }

    return ifThenElse(
        AndBoolExp(
            EqExp(FieldUse(stateFieldIdentifier), IntLiteral(t.q1.toString())),
            EqExp(
                justC(DestinyExp()),
                FieldUse(registerIdentifier(t.verb.register))
            )
        ),
        AssignStmt(
            List(), // no annotations
            FieldUse(stateFieldIdentifier),
            IntLiteral(t.q2.toString())
        ),
        elseCase
    )
        /* Result:
          if (q == X && Just(destiny) == rI) { // TODO: Aussagekräftig genug? Werden Register je geleert? Vielleicht relevant nicht im Protokoll genannte Funktionen auch aufgerufen werden können
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
 * @param transitionStmt this should be generated with [genReactivationStateTransition]
 */
private fun genAwaitReplacementForReactivation(awaitStmt: AwaitStmt, transitionStmt: Stmt) =
    Block(
        List(), //no annotations
        List(
            awaitStmt.treeCopyNoTransform(),
            transitionStmt.treeCopyNoTransform()
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
 * @param transitionStmt this should be generated by [genReactivationStateTransition]
 */
private fun genSuspendReplacementForReactivation(suspendStmt: SuspendStmt, transitionStmt: Stmt) =
    Block(
        List(), //Annotations
        List(
            suspendStmt.treeCopyNoTransform(),
            transitionStmt.treeCopyNoTransform()
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
 * @param transitionStmt this should be generated by [genReactivationStateTransition]
 * @param variableNameCount this function introduces new variables. The variable name is generated from this integer,
 *                          which should be unique for the surrounding method.
 */
private fun replaceAwaitAsyncCallForReactivation(awaitAsyncCall: AwaitAsyncCall, transitionStmt: Stmt, variableNameCount: Int) {
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
                transitionStmt.treeCopyNoTransform(),
                stmtParent.treeCopyNoTransform()
            )
        )
    )
}

/**
 * Generates a case statement, which compares the automaton state a method was initially invoked in
 * with the starting states of a set of invocation transitions with post-conditions.
 *
 * If the state matches, the post-condition is checked with an assertion statement.
 *
 * Example result:
 *
 * ```
 * case invocState {
 *   0 => {
 *      assert x != null;
 *   }
 *   3 => {
 *      assert x == null;
 *   }
 *   _ => {
 *      skip;
 *   }
 * }
 * ```
 *
 * @param invocStateVarName name of the variable which stores the state the method was invoked in
 * @param invocREvTransitions invocREv transitions with post-conditions
 * @throws IllegalArgumentException if [invocREvTransitions] contains transitions which are either no invocREv transitions or which don't have a post-condition
 */
private fun genPostConditionAssertionCases(invocStateVarName: String, invocREvTransitions: Set<Transition>) =
    CaseStmt(
        List(), // no annotations
        VarUse(invocStateVarName),
        List(
            *(
                invocREvTransitions.map{t ->
                    CaseBranchStmt(
                        LiteralPattern(IntLiteral(t.q1.toString())),
                        Block(
                            List(), // no annotations
                            List(
                                genAssertionStmtFromTransition(t)
                            )
                        )
                    )
                } + List( // Do nothing, if no post-conditions have been specified for the remaining transitions
                    CaseBranchStmt(
                        UnderscorePattern(),
                        Block(List(), List())
                    )
                )
            ).toTypedArray()
        )
    )


/**
 * Generates an ABS assertion statement which is meant to check post-conditions at runtime for
 * a InvocREv transition of an session automaton.
 *
 * Example result:
 *
 * ```
 * assert x != null
 * ```
 *
 * @throws IllegalArgumentException if the [transition.verb] is not of InvocREv type or if [transition.verb.postCondition] is null i. e. there is no post-condition defined.
 * @param transition transition containing the post-condition to check
 * @return assertion statement as ABS AST node representing
 */
private fun genAssertionStmtFromTransition(transition: Transition) =
    when (transition.verb) {
        is TransitionVerb.InvocREv ->
            transition.verb.postCondition
                ?.let { postCondition ->
                    Block(
                        List(),
                        List(
                            ifThenElse(
                                NegExp(postCondition.treeCopyNoTransform()),
                                ExpressionStmt(
                                    List(),
                                    callFun(
                                        "println",
                                        StringLiteral("A postcondition of method ${transition.verb.method.value} is about to fail.")
                                    )
                                )
                            ),
                            AssertStmt(
                                List(), // no annotations
                                postCondition
                            )
                        )
                    )
                }
                ?: throw IllegalArgumentException("The transition contains no post-condition (is null).")
        else -> throw IllegalArgumentException("Transition parameter is not an InvocREv transition, but assertions for post-conditions can only be generated for InvocREv transitions.")
    }

/**
 * Generate ABS code to check a post-conditions of InvocREv transitions by assertions before exiting via
 * a return statement.
 *
 * Example:
 *
 * return statement:
 * ```
 * return 42;
 * ```
 *
 * Produced replacement:
 * ```
 * {
 *   Int result = 42;
 *
 *   case invocState {
 *     0 => {
 *      assert this.x != null;
 *     }
 *
 *     _ => { skip; }
 *   }
 *
 *   return result;
 * }
 * ```
 *
 * @parem returnStmt return statement AST node for which a replacement is generated
 * @param invocStateVarName name of the variable which stores the automaton state during initial invocation of the method
 * @param invocREvTransitions list of invocREv transitions with post-conditions
 * @return block statement which can be used to replace the original return statement.
 * @throws IllegalArgumentException if [invocREvTransitions] contains transitions which are either no invocREv transitions or which don't have a post-condition
 */
private fun genReturnStmtPostConditionAssertReplacement(returnStmt: ReturnStmt, invocStateVarName: String, invocREvTransitions: Set<Transition>, variableNameCount: Int): ReturnStmtReplacement {
    val varName = "result$variableNameCount"
    val expTypeName = returnStmt.retExpNoTransform.type.decl.qualifiedName
    val expDeepCopy = returnStmt.retExpNoTransform.treeCopyNoTransform()

    val varDecl = VarDeclStmt(
        List(), // no annotations
        VarDecl(
            varName,
            UnresolvedTypeUse(
                expTypeName,
                List() // no annotations
            ),
            Opt(expDeepCopy)
        )
    )

    return ReturnStmtReplacement(
        originalReturnStmt = returnStmt,
        leadingStmts = listOf(
            varDecl,
            genPostConditionAssertionCases(invocStateVarName, invocREvTransitions)
        ),
        returnReplacement = ReturnStmt(
            returnStmt.annotationListNoTransform.treeCopyNoTransform(), // copy annotations from original return statement
            VarUse(varName)
        )
    )
}

// TODO KDoc
data class ReturnStmtReplacement(
    val originalReturnStmt: ReturnStmt,
    val leadingStmts: List<Stmt>,
    val returnReplacement: ReturnStmt
) {
    fun apply(methodBlock: Block) {
        if (leadingStmts.isEmpty()) {
            originalReturnStmt.replaceWith(returnReplacement)
        }

        else {
            originalReturnStmt.replaceWith(leadingStmts.head)

            leadingStmts.tail.forEach{methodBlock.addStmtNoTransform(it)}

            methodBlock.addStmtNoTransform(returnReplacement)
        }
    }
}

/**
 * Generates a variable declaration storing the current state of the automaton.
 *
 * Example:
 *
 * ```
 * Int stateCache = this.q;
 * ```
 *
 * @param varName name of the declared variable
 * @return variable declaration AST node
 */
private fun genStateCache(varName: String) =
    VarDeclStmt(
        List(), // no annotations
        VarDecl(
            varName,
            UnresolvedTypeUse(
                "Int", // TODO refactor into outside definition
                List() // no annotations
            ),
            Opt(FieldUse(stateFieldIdentifier))
        )
    )
