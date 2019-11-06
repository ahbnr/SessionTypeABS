package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.ADTConstructor
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import de.ahbnr.sessiontypeabs.intersperse

import org.abs_models.frontend.ast.*

object OfferRule: StmtsRule {
    override val name = "Offer"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        typeHead is MethodLocalType.Offer &&
        !isCommunicationInert(stmtsHead, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * the next statement is relevant to the protocol
        // * the current type is an offer type
        //
        // We need to confirm that
        //
        // (a) the next statement is a Case-Statement
        // (b) the Case-Statement's head expression is a local variable
        // (c) this local variable is part of the variables, where the result of the choosing future has been stored
        // (d) there are as many branches in the Case-Statement, as are there branches in the type
        // (e) every pattern of the Case-Statement is a constructor
        // (f) this constructor is specified in a branch of the type
        //
        // (g) Furthermore, for matching constructors of Case-Statement and type branches, we must continue the analysis on
        // the branch's statements and the statements after the Case-Statement with the corresponding branch type and
        // remaining type.

        val offerType = typeHead as MethodLocalType.Offer

        // (a)
        if (stmtsHead is CaseStmt) {
            val caseCondExp = stmtsHead.exprNoTransform

            // (b) and (c)
            if (caseCondExp is VarUse && caseCondExp.name in env.GetVariables.getOrDefault(offerType.f, emptySet())) {
                val caseBranches = stmtsHead
                    .branchsNoTransform
                    .filter { caseBranchStmt ->
                        !(
                            caseBranchStmt.leftNoTransform is UnderscorePattern &&
                            caseBranchStmt.rightNoTransform.let { right ->
                                right is Block
                                right.stmtsNoTransform.toList().let { stmts ->
                                    stmts.size == 1 &&
                                    stmts.firstOrNull()?.let { maybeThrowStmt ->
                                        maybeThrowStmt is ThrowStmt &&
                                        maybeThrowStmt.reason.let { reason ->
                                            reason is DataConstructorExp &&
                                                reason.decl.qualifiedName == "ABS.StdLib.Exceptions.PatternMatchFailException"
                                        }
                                    } ?: false
                                }
                            }
                        )
                    }

                // (d)
                if (caseBranches.size == offerType.branches.size) {
                    for (caseBranch in caseBranches) {
                        val pattern = caseBranch.leftNoTransform

                        // (e)
                        if (pattern is ConstructorPattern) {
                            val matchingBranchType = offerType.branches[ADTConstructor(pattern.constructor)]

                            // (f)
                            if (matchingBranchType != null) {
                                // (g)
                                checkStmts(
                                    env,
                                    caseBranch.rightNoTransform.stmtsNoTransform.toList() + stmtsTail,
                                    matchingBranchType concat typeTail
                                )
                            }

                            else {
                                throw ModelAnalysisException(
                                    """|The protocol specification contains a branching type, but it specifies no
                                       |behavior for the constructor ${pattern.constructor}, as does the model.
                                       |
                                       |The following constructors are permitted by the specification:
                                       |${offerType.branches.keys.map(ADTConstructor::value).intersperse(", ")}
                                       |""".trimMargin()
                                )
                            }
                        }

                        else {
                            throw ModelAnalysisException(
                                """|Case Statements implementing the branching evaluation of a future result from the specification
                                   |may only use constructors as patterns.
                                   |""".trimMargin()
                            )
                        }
                    }
                }

                else {
                    throw ModelAnalysisException(
                        """|There are not as many branches in a Case Statement as specified in the protocol.
                           |""".trimMargin()
                    )
                }
            }

            else {
                throw ModelAnalysisException(
                    """|Case Statements implementing the branching evaluation of a future result from the specification
                       |may only operate on local variables as their conditional expression, in which the result of this
                       |future has been stored.
                       |
                       |Instead, we found a Case-Statement which operates on the following expression:
                       |$caseCondExp
                       |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException(
                """|The protocol specification requires, that there must be a Case-Statement evaluating the results of
                   |future ${offerType.f}.
                   |
                   |However, we encountered no Case Statement at this point. Instead we found the following statement:
                   |$stmtsHead
                   |""".trimMargin()
            )
        }
    }
}
