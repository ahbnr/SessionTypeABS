package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.Block
import org.abs_models.frontend.ast.Stmt

object BlockRule: StmtsRule {
    override val name = "Block"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        stmtsHead is Block &&
        !isCommunicationInert(stmtsHead, env) &&
        typeHead !is MethodLocalType.Choice

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * We operate on a block of statements
        // * that block contains statements relevant to the protocol specification
        //
        // We need to continue on the contents of the block and the full type

        val block = stmtsHead as Block

        checkStmts(
            env,
            block.stmtsNoTransform + stmtsTail,
            typeHead concat typeTail
        )
    }
}
