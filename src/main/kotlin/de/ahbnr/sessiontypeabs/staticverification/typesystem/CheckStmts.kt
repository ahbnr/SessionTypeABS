package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.*
import de.ahbnr.sessiontypeabs.tail
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules.AssignDebugRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules.NoCommInertExpStmtRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules.NoGetNoCommInertVarDeclRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules.NoNewRule
import org.abs_models.frontend.ast.*

/**
 * Notes on the calculus:
 *
 * * VarAssignRule and FieldAssignRule have been downgraded to a debug rules. isCommInert can do it.
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
        // contrary to the calculus, we also allow the Skip type, to emulate the skip rule on empty statements.
        // This is ok, since method types always end with put.

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

