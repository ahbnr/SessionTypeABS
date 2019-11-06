package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isConstructorOfType
import org.abs_models.frontend.ast.DataTypeDecl
import org.abs_models.frontend.ast.ReturnStmt
import org.abs_models.frontend.ast.Stmt

object ReturnRule: StmtsRule {
    override val name = "ReturnStmt"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is ReturnStmt &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        if (stmtsTail.isEmpty()) {
            if (typeHead is MethodLocalType.Resolution) {
                if (isCommunicationInert(
                        (stmtsHead as ReturnStmt).retExpNoTransform,
                        env
                    )
                ) {
                    when {
                        // ReturnNoMsg rule
                        typeHead.constructor == null -> {
                            // OK, nothing left to do, axiom rule
                        }
                        // ReturnMsg
                        stmtsHead.retExpNoTransform.type.decl.let { decl ->
                            decl is DataTypeDecl &&
                                isConstructorOfType(
                                    typeHead.constructor,
                                    decl
                                )
                        } -> {
                            // OK, nothing left to do, axiom rule
                        }
                        else -> throw ModelAnalysisException(
                            """|A constructor specified in the protocol does not match the return type of the
                               |corresponding method.
                               |""".trimMargin()
                        )
                    }
                }

                else {
                    throw ModelAnalysisException(
                        """|Expressions in return statements of functions involved in a session type protocol may not have any
                           |side-effects on the protocol.
                           |
                           |However, we found a return statement, where this is the case.
                           |
                           |Offending statement: $stmtsHead
                           |""".trimMargin()
                    )
                }
            }

            else {
                throw ModelAnalysisException(
                    """|When encountering a return statement, there must be a resolving type in the specification
                   |corresponding to this point of the execution.
                   |
                   |However, we encountered the following type instead:
                   |$typeHead
                   |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException("After a return statement, there may be no more additional statements.")
        }
    }
}
