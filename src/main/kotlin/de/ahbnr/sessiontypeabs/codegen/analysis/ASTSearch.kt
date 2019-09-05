package de.ahbnr.sessiontypeabs.codegen.analysis

import org.abs_models.frontend.ast.*

// Functions to help find specific nodes in an AST snippet

fun <T> findAwaits(n: ASTNode<T>): List<AwaitStmt>
        where T : ASTNode<*> = n.findChildren(AwaitStmt::class.java)

fun <T> findAwaitAsyncCall(n: ASTNode<T>): List<AwaitAsyncCall>
        where T : ASTNode<*> = n.findChildren(AwaitAsyncCall::class.java)

fun <T> findSuspends(n: ASTNode<T>): List<SuspendStmt>
        where T : ASTNode<*> = n.findChildren(SuspendStmt::class.java)

fun <T> findSyncCalls(n: ASTNode<T>): List<SyncCall>
        where T : ASTNode<*> = n.findChildren(SyncCall::class.java)

/* Functions to find the end of execution for methods
   This includes:

   * return
   * throw
   * end of block in Unit methods

   TODO check papers for other possible exit points
 */

fun <T> findReturnStmt(n: ASTNode<T>): ReturnStmt?
        where T : ASTNode<*> = n.findChildren(ReturnStmt::class.java).firstOrNull()

fun <T> findThrowStmts(n: ASTNode<T>): List<ThrowStmt>
    where T : ASTNode<*> = n.findChildren(ThrowStmt::class.java)

/**
 * Returns index of last statement in a block of statements.
 *
 * @param b block of statements
 * @return index of last statement in block or null, if the block is empty
 */
fun findIndexOfLastStmt(b: Block): Int? =
    if (b.numStmtNoTransform > 1) {
        b.numStmtNoTransform - 1
    }

    else {
        null
    }

