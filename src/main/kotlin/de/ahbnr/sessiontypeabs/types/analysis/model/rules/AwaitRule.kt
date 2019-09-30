package de.ahbnr.sessiontypeabs.types.analysis.model.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsEnvironment
import de.ahbnr.sessiontypeabs.types.analysis.model.StmtsRule
import de.ahbnr.sessiontypeabs.types.analysis.model.checkStmts
import org.abs_models.frontend.ast.AwaitStmt
import org.abs_models.frontend.ast.ClaimGuard
import org.abs_models.frontend.ast.FieldUse
import org.abs_models.frontend.ast.Stmt

object AwaitRule: StmtsRule {
    override val name = "Await"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is AwaitStmt &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on an AwaitStmt
        //
        // We need to confirm that
        //
        // (a) the type specifies a release of control/suspension for this point of the execution
        // (b) the guard of the await statement is a single future
        // (c) the future specified in the guard is stored in a field
        // (d) the future specified in the guard is the same as the one the type specifies to wait on
        //
        // (e) Furthermore we need to continue on the remaining statements and remaining type

        val awaitStmt = stmtsHead as AwaitStmt

        // (a)
        if (typeHead is MethodLocalType.Suspension) {
            val guard = awaitStmt.guardNoTransform

            // (b)
            if (guard is ClaimGuard) {
                val claim = guard.varNoTransform

                // (c)
                if (claim is FieldUse) {
                    // (d)
                    if (claim.name == typeHead.awaitedFuture.value) {
                        checkStmts(
                            env,
                            stmtsTail,
                            typeTail
                        )
                    }

                    else {
                        throw ModelAnalysisException(
                            """|Found an await statement which does not use the same future in its guard as in the
                               |specification.
                               |The specification expected the use of future ${typeHead.awaitedFuture.value}, but the
                               |await statement waits on ${claim.name}.
                               |""".trimMargin()
                        )
                    }
                }

                else {
                    throw ModelAnalysisException(
                        """|In Session Type ABS futures relevant to a session type protocol must be stored in fields.
                           |Thus, await statements must also operate on fields, but we found a statement where this
                           |is not the case.
                           |""".trimMargin()
                    )
                }
            }

            else {
                throw ModelAnalysisException(
                    """|In Session Type ABS, guards of await statements may only be a single future.
                       |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException(
                """|When encountering an Await-Statement, there must be a releasing type in |the specification
                   |corresponding to this point of the execution.
                   |
                   |However, we encountered the following (local) type instead:
                   |$typeHead
                   |""".trimMargin()
            )
        }
    }
}
