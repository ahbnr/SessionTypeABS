package de.ahbnr.sessiontypeabs.types.analysis.model.rules.debug_rules

import de.ahbnr.sessiontypeabs.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.analysis.model.*
import org.abs_models.frontend.ast.*

object NoCommInertExpStmtRule: StmtsRule {
    override val name = "NoCommInertExpStmt"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ):Boolean =
        stmtsHead is ExpressionStmt &&
        !isCommunicationInert(stmtsHead.expNoTransform, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * There is an Expression-Stmt
        // * It has side-effects on the protocol
        //
        // Expression-Stmts may have no side-effects on a protocol

        throw ModelAnalysisException(
            """ |In Expression-Statments there are no expressions allowed, which have side-effects on
                |a protocol specified by a session type.
                |
                |Solution: Asynchronous calls should be part of an assignment, such that the produced future is stored
                |          in a field.
                |          Get-Expressions must always be assigned to local variables.
                |          Synchronous calls are not allowed at all.
                |
                |Offending statement: $stmtsHead
            """.trimMargin()
        )
    }
}
