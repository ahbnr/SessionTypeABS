package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import org.abs_models.frontend.ast.FieldUse
import org.abs_models.frontend.ast.GetExp
import org.abs_models.frontend.ast.Stmt
import org.abs_models.frontend.ast.VarDeclStmt

object GetVarDeclRule: StmtsRule {
    override val name = "GetVarDecl"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is VarDeclStmt &&
        stmtsHead.varDeclNoTransform.hasInitExp() &&
        stmtsHead.varDeclNoTransform.initExp is GetExp &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on a local variable declaration
        // * The value we are assigning is a Get-Expression
        //
        // We need to confirm that
        //
        // (a) that the current protocol head is a fetching type
        // (b) the get expression operates on a field
        // (c) the field's name is the name of the future used in the fetching type
        //
        // (d) Furthermore we need to continue on the remaining statements and type with a slightly changed environment:
        // The variable must be added to the set of variables used to store the result of the future mentioned in the
        // fetching type.

        // (a)
        if (typeHead is MethodLocalType.Fetching) {
            val varDecl = (stmtsHead as VarDeclStmt).varDeclNoTransform
            val getExp = varDecl.initExp as GetExp
            val assignedExpression = getExp.pureExpNoTransform

            // (b)
            if (assignedExpression is FieldUse) {
                val futureReadInProgram = Future(assignedExpression.name)

                // (c)
                if (futureReadInProgram == typeHead.f) {
                    val varName = varDecl.name

                    // (d)
                    checkStmts(
                        env.copy(
                            GetVariables = env.GetVariables + (
                                typeHead.f to (env.GetVariables.getOrDefault(typeHead.f, emptySet()) + varName)
                                )
                        ),
                        stmtsTail,
                        typeTail
                    )
                }

                else {
                    throw ModelAnalysisException(
                        """|The future read in a Get-Expression in the given program is not the same one that is
                           |specified in the Session Type protocol.
                           |""".trimMargin()
                    )
                }
            }

            else {
                throw ModelAnalysisException(
                    """|Currently, Session Type ABS only allows to store protocol relevant futures in fields.
                       |Thus, Get-Expressions may only operate on fields.
                       |
                       |Offending statement:
                       |$stmtsHead
                       |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException(
                """|When encountering a get expression, there must be a fetching type in the specification
                   |corresponding to this point of the execution.
                   |
                   |However, we encountered the following type instead:
                   |$typeHead
                   |""".trimMargin()
            )
        }
    }
}
