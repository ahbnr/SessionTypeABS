package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.tail
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.analysis.model.rules.*
import de.ahbnr.sessiontypeabs.types.analysis.model.rules.debug_rules.AssignDebugRule
import de.ahbnr.sessiontypeabs.types.analysis.model.rules.debug_rules.NoCommInertExpStmtRule
import de.ahbnr.sessiontypeabs.types.analysis.model.rules.debug_rules.NoGetNoCommInertVarDeclRule
import de.ahbnr.sessiontypeabs.types.analysis.model.rules.debug_rules.NoNewRule
import org.abs_models.frontend.ast.*

// FIXME
/**
 * Things to fix in the calculus:
 *
 * * Get expression only on fields
 * * Class parameter syntax looks wrong
 * * Await rule waits on the wrong future in the listing
 * * dont allow If or Case Stmts in Choice Rule
 * * Choice clashes with Block
 * * VarAssignRule and FieldAssignRule are downgraded to a debug rules. isCommInert can do it.
 * * return rule excludes choice
 * * MethodEnd should be renamed to StmtsEnd, and it should allow type == Skip
 */

val stmtRules = listOf(
    AwaitRule,
    BlockRule,
    CallRule,
    ChoiceRule,
    CommInertRule,
    //FieldAssignRule,
    GetAssignRule,
    GetAssignRule,
    GetVarDeclRule,
    NoOfferCaseRule,
    NoOfferIfRule,
    OfferRule,
    ReturnRule,
    //VarAssignRule,
    WhileRule,

    // Rules, which introduce debug messages helpful to the user of this tool
    NoNewRule,
    NoGetNoCommInertVarDeclRule,
    NoCommInertExpStmtRule,
    AssignDebugRule
)

fun checkStmts(env: StmtsEnvironment, stmts: List<Stmt>, type: MethodLocalType?) =
    when {
        // The rules MethodEnd and TypeEnd are built in to this method and not modeled as separate rules, since their
        // signature deviates from the one of the other rules (no guaranteed first statement or type)
        type == null -> checkTypeEnd(env, stmts)
        stmts.isEmpty() -> checkMethodEnd(env, type)
        // If none of the above cases apply, we can continue with the "normal" rules
        else -> {
            val typeHead = type.head
            val typeTail = type.tail

            val stmtsHead = stmts.head
            val stmtsTail = stmts.tail

            val applicableRules = stmtRules
                .filter {
                    it.guard(env, stmtsHead, stmtsTail, typeHead, typeTail)
                }

            when {
                applicableRules.isEmpty() ->
                    throw ModelAnalysisException(
                        """
                        |Encountered an unknown source code / session type pattern.
                        |Either you are using an unsupported ABS feature or your model is violating the protocol
                        |specification.
                        |
                        |Offending statement: $stmtsHead
                        |Current session type part: $typeHead
                        """.trimMargin()
                    )
                applicableRules.size > 1 -> throw RuntimeException(
                    "More than one analysis rule is applicable. This should never happen and is an error in Session Type ABS."
                )
                else -> applicableRules.head.invoke(env, stmtsHead, stmtsTail, typeHead, typeTail)
            }
        }
    }

fun checkMethodEnd(env: StmtsEnvironment, type: MethodLocalType) =
    if (type is MethodLocalType.Resolution || type is MethodLocalType.Skip) {
        // OK, nothing left to do, axiom rule
    }

    else {
        throw ModelAnalysisException(
            """
                The protocol specification describes additional behavior, which can not be found in the model.
                
                Additional behavior specified in the protocol, which is not implemented by the model:
                $type
            """.trimIndent()
        )
    }

fun checkTypeEnd(env: StmtsEnvironment, stmts: List<Stmt>) =
    if (stmts.all { isCommunicationInert(it, env) }) {
        // OK, nothing left to do, axiom rule
    }

    else {
        throw ModelAnalysisException(
            "There is additional behavior in a method, which is not captured by the protocol specification."
        )
    }


//fun checkIfSupported(stmt: Stmt) =
//    when (stmt) {
//        is Block, is CaseStmt, is IfStmt, is AwaitStmt, is AssertStmt, is ExpressionStmt, is ReturnStmt, is AssignStmt, is VarDeclStmt, is SkipStmt, is WhileStmt, is ForeachStmt
//        -> true
//        is SuspendStmt
//        -> throw ModelAnalysisException("In methods participating in a Session Type, no unconditional suspend statements are allowed.")
//        is ThrowStmt
//        -> throw ModelAnalysisException("Throwing exceptions is currently not supported by Session Type ABS.")
//        is TryCatchFinallyStmt
//        -> throw ModelAnalysisException("Handling exceptions (try-catch-finally) is currently not supported by Session Type ABS.")
//        else
//        -> throw ModelAnalysisException("The statement $stmt is currently not supported by Session Type ABS, or at least not supported in this part of the ABS model.")
//    }
//
//fun checkIfSupported(exp: EffExp) =
//    when (exp) {
//        is GetExp, is AsyncCall, is AwaitAsyncCall, is NewExp // FIXME handle new expression as call to run / init
//        -> true
//        is SyncCall
//        -> throw ModelAnalysisException("Synchronous calls are not supported in Session Type ABS, since it encompasses suspension of its whole COG, which can not be represented with a Session Type at the time.")
//        is OriginalCall
//        -> throw ModelAnalysisException("Traits are currently not supported by Session Type ABS, thus a `original` call is also not supported.")
//        else
//        -> throw ModelAnalysisException(
//            """
//                    Found an expression which is neither an EffExp nor pure. Thus, it is not supported by Session Type ABS.
//                    Offending expression: $exp
//                """.trimIndent().trimMargin()
//        )
//    }
