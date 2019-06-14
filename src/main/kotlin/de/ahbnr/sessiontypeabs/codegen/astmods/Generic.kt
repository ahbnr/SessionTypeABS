package de.ahbnr.sessiontypeabs.codegen.astmods

import org.abs_models.frontend.ast.*

// This file contains miscellaneous functions for modifying ABS AST nodes

/**
 * Prepends a Stmt node in front of all other statments of a Block
 */
fun Block.prependStmt(stmt: Stmt) {
    val origStmtList = this.stmtListNoTransform

    // TODO: Inserting a node in front of the list does not work without a full
    // tree copy. I do not yet know why.
    val newStmtList = origStmtList.treeCopyNoTransform()
    newStmtList.insertChild(stmt, 0)

    this.stmtList = newStmtList
}

