package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.AssignStmt
import org.abs_models.frontend.ast.GetExp
import org.abs_models.frontend.ast.Stmt
import org.abs_models.frontend.ast.VarUse

object VarAssignRule: StmtsRule {
    override val name = "VarAssign"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is AssignStmt &&
            stmtsHead.varNoTransform is VarUse &&
            stmtsHead.valueNoTransform !is GetExp

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on an AssignStmt
        // * We are assigning a variable, not a field
        // * The value we are assigning is not a Get-Expression
        //
        // We need to confirm that
        //
        // (a) the assigned variable hasnt been assigned a Get-Expression before
        // (b) The value we are assigning has no side-effects on the protocol (no relevant call expression etc.)
        //
        // (c) Furthermore we need to continue on the remaining statements and full type

        val assignStmt = stmtsHead as AssignStmt
        val varName = assignStmt.varNoTransform.name

        // (a)
        if (env.GetVariables.values.none { varName in it }) {
            // (b)
            if (isCommunicationInert(
                    stmtsHead.valueNoTransform,
                    env
                )
            ) {
                // (c)
                checkStmts(
                    env,
                    stmtsTail,
                    typeHead concat typeTail
                )
            }

            else {
                throw ModelAnalysisException(
                    """|Trying to assign a variable, but the assigned expression has side-effects and is not a Get-Expression.
                       |
                       |Assignments to local variables are only ok, if the assigned expression is a Get Expression
                       |specified in the Session Type, or it has no side-effects.
                       |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException(
                """|Trying to assign a variable, which already holds the result of a Get-Expression.
                   |
                   |However, after being assigned the result of a get-Expression, a variable may never be assigned another value again.
                   |""".trimMargin()
            )
        }
    }
}
