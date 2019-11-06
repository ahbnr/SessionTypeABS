package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.checkStmts
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isCommunicationInert
import de.ahbnr.sessiontypeabs.types.concat
import org.abs_models.frontend.ast.Stmt

object CommInertRule: StmtsRule {
    override val name = "CommInert"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) =
        isCommunicationInert(stmtsHead, env)

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * stmtsHead contains no statements or expressions affecting the protocol
        //
        // We need to continue on the remaining statements and the full type

        checkStmts(
            env,
            stmtsTail,
            typeHead concat typeTail
        )
    }
}
