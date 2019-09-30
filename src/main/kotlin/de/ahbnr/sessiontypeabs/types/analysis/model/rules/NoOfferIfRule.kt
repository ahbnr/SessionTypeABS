package de.ahbnr.sessiontypeabs.types.analysis.model.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsEnvironment
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsRule
import de.ahbnr.sessiontypeabs.types.analysis.model.checkStmts
import de.ahbnr.sessiontypeabs.types.analysis.model.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.IfStmt
import org.abs_models.frontend.ast.Stmt

object NoOfferIfRule: StmtsRule {
    override val name = "noOfferIf"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        typeHead !is MethodLocalType.Offer &&
            stmtsHead is IfStmt &&
            !isCommunicationInert(stmtsHead, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * the next statement is relevant to the protocol
        // * it is an If or IfElse statement
        // * (the current type is not an offer type)
        //
        // We need to confirm that no matter which path the program chooses,
        // it is covered by the specification

        val ifStmt = stmtsHead as IfStmt

        checkStmts(
            env,
            ifStmt.thenNoTransform.stmtsNoTransform + stmtsTail,
            typeHead concat typeTail
        )

        checkStmts(
            env,
            if (ifStmt.hasElse() && ifStmt.`else` != null) {
                ifStmt.`else`.stmtsNoTransform.toList()
            }
            else {
                emptyList()
            } + stmtsTail,
            typeHead concat typeTail
        )
    }
}
