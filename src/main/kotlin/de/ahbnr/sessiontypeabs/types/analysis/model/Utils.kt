package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.types.ADTConstructor
import org.abs_models.frontend.ast.*

fun isConstructorOfType(constructor: ADTConstructor, typeDecl: DataTypeDecl) =
    typeDecl
        .dataConstructorsNoTransform
        .any {
            it.name == constructor.value || it.qualifiedName == constructor.value
        }

fun isNewCommunicationInert(
    newExp: NewExp,
    environment: StmtsEnvironment
): Boolean =
    !environment.isActor(newExp)

fun <T> isCommunicationInert(
    node: ASTNode<T>,
    environment: StmtsEnvironment
): Boolean
    where T : ASTNode<*> =
   when (node) {
        is SkipStmt,
        is AssertStmt,
        is DurationStmt,
        is PureExp -> true
        is Block ->
            node.stmtsNoTransform.all { isCommunicationInert(it, environment) }
        is ExpressionStmt
            -> isCommunicationInert(node.expNoTransform, environment)
        is IfStmt
            -> isCommunicationInert(node.thenNoTransform, environment) &&
                if (node.hasElse() && node.`else` != null) {
                    isCommunicationInert(node.`else`, environment)
                }

                else {
                    true
                }
        is CaseStmt
            -> node.branchsNoTransform.all { isCommunicationInert(it.rightNoTransform, environment) }
        is WhileStmt
            -> isCommunicationInert(node.bodyNoTransform, environment)
        is VarDeclStmt ->
            if (node.varDeclNoTransform.hasInitExp() && node.varDeclNoTransform.initExp != null) {
                isCommunicationInert(node.varDeclNoTransform.initExp, environment)
            } else {
                true // FIXME: Should be true?
            }
        is AssignStmt -> {
                val assignedVarOrField = node.varNoTransform
                val assignedValue = node.valueNoTransform

                (assignedVarOrField !is FieldUse || !environment.doesFieldStoreFuture(assignedVarOrField)) &&
                (assignedVarOrField !is VarUse || !environment.doesVariableStoreAGetValue(assignedVarOrField)) &&
                isCommunicationInert(assignedValue, environment)
            }
        is Call -> !environment.isActor(node.calleeNoTransform) // TODO: Does not allow calling methods, which are not specified in the protocol. Do we want this?
        is NewExp -> isNewCommunicationInert(node, environment)
        else -> false
    }


