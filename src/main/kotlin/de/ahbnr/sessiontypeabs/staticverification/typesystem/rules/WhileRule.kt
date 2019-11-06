package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import org.abs_models.frontend.ast.Stmt
import org.abs_models.frontend.ast.WhileStmt

object WhileRule: StmtsRule {
    override val name = "While"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is WhileStmt &&
        !isCommunicationInert(stmtsHead.bodyNoTransform, env) &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on a while loop
        // * the loop body is relevant to the protocol
        //
        // We need to confirm that
        //
        // (a) that the current protocol head is a repeating type
        //
        // Furthermore we need to continue on
        //
        // (b) the loop body with the repeated type
        // (c) the remaining statements with the remaining type

        // (a)
        if (typeHead is MethodLocalType.Repetition) {
            val loop = stmtsHead as WhileStmt

            // (b)
            checkStmts(
                env,
                loop.bodyNoTransform.stmtsNoTransform.toList(),
                typeHead.repeatedType
            )

            // (c)
            checkStmts(
                env,
                stmtsTail,
                typeTail
            )
        }

        else {
            throw ModelAnalysisException(
                """|When encountering a loop, whose content is relevant to the protocol specification, there must be a
                   |repeated type in the specification corresponding to this point of the execution.
                   |
                   |However, we encountered the following type instead:
                   |$typeHead
                   |""".trimMargin()
            )
        }
    }
}
