package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.Block
import org.abs_models.frontend.ast.CaseStmt
import org.abs_models.frontend.ast.IfStmt
import org.abs_models.frontend.ast.Stmt

object ChoiceRule: StmtsRule {
    override val name = "Choice"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        typeHead is MethodLocalType.Choice &&
            stmtsHead !is IfStmt &&
            stmtsHead !is CaseStmt &&
            stmtsHead !is Block &&
            !isCommunicationInert(stmtsHead, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * the next statement is relevant to the protocol
        // * the current type is a choice type
        // * (the next statement is no direct branching statement or a block)
        //
        // We need to confirm that
        //
        // (a) there is a branch of the choice type, that types the behavior of the model

        val choiceType = typeHead as MethodLocalType.Choice

        // (a)
        choiceType.choices.any {choice ->
            try {
                checkStmts(
                    env,
                    listOf(stmtsHead) + stmtsTail,
                    choice concat typeTail
                )

                true
            }

            catch (me: ModelAnalysisException) {
                false
            }
        }
    }
}
