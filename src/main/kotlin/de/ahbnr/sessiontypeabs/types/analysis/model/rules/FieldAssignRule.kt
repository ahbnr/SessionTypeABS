package de.ahbnr.sessiontypeabs.types.analysis.model.rules

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsEnvironment
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsRule
import de.ahbnr.sessiontypeabs.types.analysis.model.checkStmts
import de.ahbnr.sessiontypeabs.types.analysis.model.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.AssignStmt
import org.abs_models.frontend.ast.FieldUse
import org.abs_models.frontend.ast.Stmt

object FieldAssignRule: StmtsRule {
    override val name = "FieldAssign"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is AssignStmt &&
            stmtsHead.varNoTransform is FieldUse &&
            isCommunicationInert(stmtsHead.valueNoTransform, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on an AssignStmt
        // * We are assigning a field, not a variable
        // * The value we are assigning has no side-effects on the specified protocol
        //
        // We need to confirm that
        //
        // (a) the assigned field has not the name of a future, to prevent us from overwriting a future
        //
        // (b) Furthermore we need to continue on the remaining statements and full type

        val assignStmt = stmtsHead as AssignStmt
        val fieldName = assignStmt.varNoTransform.name

        // (a)
        if (Future(fieldName) !in env.Futures) {
            // (b)
            checkStmts(
                env,
                stmtsTail,
                typeHead concat typeTail
            )
        }

        else {
            throw ModelAnalysisException(
                """|Trying to assign an expression which is not a call to a field, which has the name of a future.
                   |
                   |This is not allowed in Session Type ABS, since fields with future names are reserved to store the
                   |future with that name.
                   |""".trimMargin()
            )
        }
    }
}
