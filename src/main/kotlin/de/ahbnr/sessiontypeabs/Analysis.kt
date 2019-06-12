package de.ahbnr.sessiontypeabs

import org.abs_models.frontend.ast.*

/**
 * NOTES
 *
 * Statements and expressions potentially relevant to ReactEv events:
 *
 * Precondition: We ignore adding objects to an existing cog via `new local`
 *   TODO: Discuss options of lifting this restriction and its implications.
 *
 * - AwaitStmt
 * - AwaitAsyncCall
 * - SuspendStmt
 * - SyncCall:
 *   * blocks execution in the local COG, no other processes in the object
 *     can run during the call, therefore no reactivation transitions must be
 *     implemented afterwards.
 *   * However, all potential reactivation calls in the called method must be
 *     considered.
 *     Three Cases:
 *       (1) Callee object implements an interface of the surrounding class, in
 *           which case the current class must be searched for the called method
 *           and the method must be examined for futher reactivation points and
 *           async calls.
 *           If a method was called synchronously can always be checked during
 *           reactivation transitions by comparing `thisDestiny` to the registers.
 *       (2) Callee object is `this`. If the surrounding class implements an
 *           interface, case (1) can be applied.
 *           If not then the class is irrelevant regarding SessionTypes anyway,
 *           since it can not be part of any protocol.
 *       (3) Callee object implements an interface which is not implemented by
 *           the surrounding class. Therefore the callee must belong to another
 *           COG and the own COG is simply blocked. => Case not relevant to local
 *           scheduling.
 *
 *           Citation ABSCore: Only asynchronous method calls can occur between
 *           different cogs, different cogs have no shared heap.
 *
 * - GetExp: No reactivation point, blocks the local COG and is therefore irrelevant to local scheduling.
 *   TODO: However: We might want to throw an error/warning if get is used on a
 *   method of the local COG, since it will block indefinetly and therefore
 *   break SessionTypes.
 * - new Expression:
 *   * .init Block: Should be called synchronously and block the local COG, therefore
 *     irrelevant. TODO: Confirm this!
 *   * run method: Is called asynchronously in another COG, therefore not relevant
 *     and no reactivation.
 *
 * TODO: Implications of inheritance and `original` calls on SessionTypeABS and
 * reactivation in general.
 */

fun <T> findAwaits(n: ASTNode<T>): List<AwaitStmt>
        where T : ASTNode<*> = n.findChildren(AwaitStmt::class.java)

fun <T> findAwaitAsyncCall(n: ASTNode<T>): List<AwaitAsyncCall>
        where T : ASTNode<*> = n.findChildren(AwaitAsyncCall::class.java)

fun <T> findSuspends(n: ASTNode<T>): List<SuspendStmt>
        where T : ASTNode<*> = n.findChildren(SuspendStmt::class.java)

fun <T> findSyncCalls(n: ASTNode<T>): List<SyncCall>
        where T : ASTNode<*> = n.findChildren(SyncCall::class.java)

/**
 * Check whether a SyncCall calls into an object of a specific interface
 */
fun callsIntoInterface(call: SyncCall, interfaceDecl: InterfaceDecl) =
    call
        .calleeNoTransform
        .type
        .decl
        .qualifiedName == interfaceDecl.qualifiedName

/**
 * Returns the implementation of the method called by a given SyncCall object
 * if the callee implements one of the interfaces of the given class
 */
fun methodOfLocalSyncCall(call: SyncCall, context: ClassDecl): MethodImpl? {
    val methods = (
            if (context.superTypes.any{ interfaceDecl -> callsIntoInterface(call, interfaceDecl)}){
                context.methodsNoTransform
            }

            else {
                List()
            }
        )

    return methods
        .find{m -> call.method == m.methodSigNoTransform.name}
}

/**
 * ADT for storing possible reactivation points of a method.
 */
sealed class ReactivationPoint {
    class Await(
        val awaitStmt: AwaitStmt
    ): ReactivationPoint()

    class AwaitAsyncCall(
        val awaitAsyncCall: org.abs_models.frontend.ast.AwaitAsyncCall
    ): ReactivationPoint()

    class Suspend(
        val suspendStmt: SuspendStmt
    ): ReactivationPoint()
}

/**
 * Search a method and other methods synchronously called by it potentially in
 * the same COG for reactivation points.
 *
 * The third mutable set parameter should initially be called with an empty set,
 * since it is only used to avoid non-termination due to call circles during the
 * search.
 */
fun findReactivationPoints(method: MethodImpl, context: ClassDecl, exploredMethods: MutableSet<String>): List<ReactivationPoint> {
    val name = method.methodSig.name
    if (exploredMethods.contains(name)) {
        return emptyList()
    }

    // avoid circles.
    exploredMethods.add(name)

    val syncCalls = findSyncCalls(method)
    val syncCallMethods = syncCalls
        .mapNotNull { call -> methodOfLocalSyncCall(call, context) }
        .filter{calledMethod -> !exploredMethods.contains(calledMethod.methodSig.name)}

    return (
            findAwaits(method).map{a -> ReactivationPoint.Await(a)}
                    + findAwaitAsyncCall(method).map{asc -> ReactivationPoint.AwaitAsyncCall(asc)}
                    + findSuspends(method).map{s -> ReactivationPoint.Suspend(s)}
                    // recursion on synchronous calls
                    + syncCallMethods.map{m -> findReactivationPoints(m, context, exploredMethods)}.flatten()
            )
}
