package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import org.abs_models.frontend.ast.AssignStmt
import org.abs_models.frontend.ast.AsyncCall
import org.abs_models.frontend.ast.FieldUse
import org.abs_models.frontend.ast.Stmt

object CallRule: StmtsRule {
    override val name = "Call"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is AssignStmt &&
        stmtsHead.varNoTransform is FieldUse &&
        stmtsHead.valueNoTransform.let { assignedExpression ->
            assignedExpression is AsyncCall &&
                env.actorModelMapping.findActorByType(assignedExpression.calleeNoTransform.type) != null
        } &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on an AssignStmt
        // * We are assigning a field, not a variable
        // * the value we are assigning is an asynchronous call
        // * we are calling a participant of the protocol
        //
        // We need to confirm that
        //
        // (a) the type specifies a call for this point of the execution
        // (b) the expression representing the callee has an interface type of the actor specified in the call type
        // (c) the called method is the same as specified
        // (d) the assigned field has the name of the future specified in the call type
        //
        // (e) Furthermore we need to continue on the remaining statements and remaining type

        val assignStmt = stmtsHead as AssignStmt
        val fieldName = assignStmt.varNoTransform.name

        // (a)
        if (typeHead is MethodLocalType.Sending) {
            val call = assignStmt.valueNoTransform as AsyncCall
            val callee = env.actorModelMapping.findActorByType(call.calleeNoTransform.type)
                ?: throw RuntimeException("Couldn't find the protocol participant for an interface type, but we already checked that there must be one. Therefore this is a programming error.")

            // (b)
            if (callee == typeHead.receiver) {
                // (c)
                if (call.methodSig.name == typeHead.m.value) {
                    // (d)
                    if (fieldName == typeHead.f.value) {
                        // (e)
                        checkStmts(
                            env,
                            stmtsTail,
                            typeTail
                        )
                    }

                    else {
                        throw ModelAnalysisException(
                            """|The futures resulting from calls relevant to the protocol always have to be stored in
                               |fields of the same name.
                               |
                               |However, future ${typeHead.f.value} is stored in a field of the name $fieldName.
                               |
                               |Offending statement: $stmtsHead
                               |""".trimMargin()
                        )
                    }
                }

                else {
                    throw ModelAnalysisException(
                        """|Called method in a call expression is not the one specified in the protocol.
                           |
                           |Instead of ${typeHead.m.value}, ${call.methodSig.name} is called.
                           |""".trimMargin()
                    )
                }
            }

            else {
                throw ModelAnalysisException(
                    """|Callee in a call expression is not the one specified in the protocol.
                       |
                       |Instead of ${typeHead.receiver}, $callee is called.
                       |""".trimMargin()
                )
            }
        }

        else {
            throw ModelAnalysisException(
                """|When encountering a call expression to a protocol participant, there must be an interaction type in
                   |the specification corresponding to this point of the execution.
                   |
                   |However, we encountered the following type instead:
                   |$typeHead
                   |
                   |Offending statement:
                   |$stmtsHead
                   |""".trimMargin()
            )
        }
    }
}
