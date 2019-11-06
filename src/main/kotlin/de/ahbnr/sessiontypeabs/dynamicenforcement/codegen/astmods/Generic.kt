package de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.astmods

import org.abs_models.frontend.ast.*

// This file contains miscellaneous functions for modifying ABS AST nodes

/**
 * Prepends a Stmt node in front of all other statments of a Block
 */
fun Block.prependStmt(stmt: Stmt) {
    val origStmtList = this.stmtListNoTransform

    // Inserting a node in front of the list does not work without a full
    // tree copy.
    val newStmtList = origStmtList.treeCopyNoTransform()
    newStmtList.insertChild(stmt, 0)

    this.stmtList = newStmtList
}

