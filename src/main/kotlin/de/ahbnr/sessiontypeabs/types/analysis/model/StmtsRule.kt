package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.types.MethodLocalType
import org.abs_models.frontend.ast.Stmt

interface StmtsRule {
    val name: String

    fun guard(
        env: StmtsEnvironment,
        stmtsHead: Stmt,
        stmtsTail: List<Stmt>,
        typeHead: MethodLocalType,
        typeTail: MethodLocalType?
    ): Boolean

    fun invoke(
        env: StmtsEnvironment,
        stmtsHead: Stmt,
        stmtsTail: List<Stmt>,
        typeHead: MethodLocalType,
        typeTail: MethodLocalType?
    )
}
