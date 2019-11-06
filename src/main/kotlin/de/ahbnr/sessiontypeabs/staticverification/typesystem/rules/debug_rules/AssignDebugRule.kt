package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import org.abs_models.frontend.ast.*

object AssignDebugRule: StmtsRule {
    override val name = "AssignDebug"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is AssignStmt &&
        stmtsHead.valueNoTransform !is GetExp &&
        stmtsHead.varNoTransform.let {assignmentTarget ->
            (
                stmtsHead.valueNoTransform !is AsyncCall && assignmentTarget is FieldUse &&
                    (
                        env.doesFieldStoreFuture(assignmentTarget) ||
                        !isCommunicationInert(
                            stmtsHead.valueNoTransform,
                            env
                        )
                    ) ||
                assignmentTarget is VarUse &&
                    (
                        env.doesVariableStoreAGetValue(assignmentTarget) ||
                        !isCommunicationInert(
                            stmtsHead.valueNoTransform,
                            env
                        )
                    )
            )
        }

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on an AssignStmt
        // * the assigned value is not a Get-Expression
        // * Either we are assigning to a field something which is not a call and the field is reserved for futures or the assigned expression has side-effects on the protocol
        // * ...or we are assigning to a local variable which already has been assigned a Get-Expression and may not be
        //      overridden again
        // * ...or the value we are assigning to the variable has side-effects on the specified protocol
        //
        // All of the above possibilities are not allowed and we want to produce an error message.

        val assignStmt = stmtsHead as AssignStmt
        val varOrField = assignStmt.varNoTransform

        if (stmtsHead.valueNoTransform !is AsyncCall && varOrField is FieldUse && env.doesFieldStoreFuture(varOrField)) {
            throw ModelAnalysisException(
                """|Trying to assign an expression which is not a call to a field, which has the name of a future.
                   |
                   |This is not allowed in Session Type ABS, since fields with future names are reserved to store the
                   |future with that name.
                   |""".trimMargin()
            )
        }

        else if (varOrField is VarUse && env.doesVariableStoreAGetValue(varOrField)) {
            throw ModelAnalysisException(
                """|Trying to assign to a variable, which already holds the result of a Get-Expression.
                   |
                   |However, after being assigned the result of a get-Expression, a variable may never be assigned another value again.
                   |""".trimMargin()
            )
        }

        else {
            val assignedValue = assignStmt.valueNoTransform

            if (varOrField is VarUse && assignedValue is AsyncCall) {
                throw ModelAnalysisException(
                    """ |Futures created by calls must be stored in fields, if they are part of a session type protocol.
                        |
                        |Offending statement: $stmtsHead
                        |
                        |Solution: Create a field of the right future type. Then replace the local variable assignment
                        |          with an assignment to that field.
                    """.trimMargin()
                )
            }

            else {
                throw ModelAnalysisException(
                    """ |In assignments there are no expressions allowed, which have side-effects on
                        |a protocol specified by a session type, although there are the following exceptions:
                        |
                        |* Futures created by asynchronous calls can be stored in fields with the name of the future
                        |  specified in the Session Type.
                        |* Get-Expressions can be stored in local variables.
                        |
                        |Offending statement: $stmtsHead
                    """.trimMargin()
                )
            }
        }
    }
}
