package de.ahbnr.sessiontypeabs.analysis

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
