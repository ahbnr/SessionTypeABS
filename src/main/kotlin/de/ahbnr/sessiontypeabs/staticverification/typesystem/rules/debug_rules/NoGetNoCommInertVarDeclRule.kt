package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules

import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import org.abs_models.frontend.ast.*

object NoGetNoCommInertVarDeclRule: StmtsRule {
    override val name = "NoGetNoCommInertVarDecl"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ):Boolean =
        stmtsHead is VarDeclStmt &&
            stmtsHead.varDeclNoTransform.hasInitExp() &&
            stmtsHead.varDeclNoTransform.initExp !is GetExp &&
            !isCommunicationInert(
                stmtsHead.varDeclNoTransform.initExp,
                env
            )

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * There is a variable declaration with an init expression with side-effects on the protocol
        // * The init expression is no Get-Expression
        //
        // No variable declarations with side-effects on the protocol are allowed, besides Get-Expressions

        val initExp = (stmtsHead as VarDeclStmt).varDeclNoTransform.initExp

        if (initExp is AsyncCall) {
            throw ModelAnalysisException(
                """ |Futures created by calls must be stored in fields, if they are part of a session type protocol.
                    |
                    |Offending statement: $stmtsHead
                    |
                    |Solution: Create a field of the right future type. Then replace the local variable declaration
                    |          with an assignment to that field.
            """.trimMargin()
            )
        }

        else {
            throw ModelAnalysisException(
                """ |In variable declarations there are no initializing expressions allowed, which have side-effects on
                    |a protocol specified by a session type, except for Get-Expressions.
                    |
                    |Offending statement: $stmtsHead
            """.trimMargin()
            )
        }
    }
}
