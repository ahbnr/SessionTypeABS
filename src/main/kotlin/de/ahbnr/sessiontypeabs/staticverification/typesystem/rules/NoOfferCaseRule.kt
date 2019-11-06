package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.CaseStmt
import org.abs_models.frontend.ast.Stmt

object NoOfferCaseRule: StmtsRule {
    override val name = "noOfferCase"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        typeHead !is MethodLocalType.Offer &&
            stmtsHead is CaseStmt &&
            !isCommunicationInert(stmtsHead, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * the next statement is relevant to the protocol
        // * it is an Case-Statement
        // * (the current type is not an offer type)
        //
        // We need to confirm that no matter which path the program takes,
        // it is covered by the specification

        val caseStmt = stmtsHead as CaseStmt

        for (case in caseStmt.branchsNoTransform) {
            checkStmts(
                env,
                case.rightNoTransform.stmtsNoTransform + stmtsTail,
                typeHead concat typeTail
            )
        }
    }
}
