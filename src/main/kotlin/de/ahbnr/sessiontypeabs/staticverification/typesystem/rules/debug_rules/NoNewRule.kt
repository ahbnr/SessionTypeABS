package de.ahbnr.sessiontypeabs.staticverification.typesystem.rules.debug_rules

import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.types.MethodLocalType
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsEnvironment
import de.ahbnr.sessiontypeabs.staticverification.typesystem.StmtsRule
import de.ahbnr.sessiontypeabs.staticverification.typesystem.isNewCommunicationInert
import org.abs_models.frontend.ast.*

object NoNewRule: StmtsRule {
    override val name = "NoNew"

    override fun guard(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ):Boolean =
        stmtsHead.findChildren<NewExp>().any {
            !isNewCommunicationInert(it, env)
        }

    override fun invoke(env: StmtsEnvironment, stmtsHead: Stmt, stmtsTail: List<Stmt>, typeHead: MethodLocalType, typeTail: MethodLocalType? ) {
        // When this rule is invoked, we know the following
        //
        // * There is a new expression in the next statement, which instantiates an actor from the protocol
        //
        // New expressions are not allowed within methods relevant to the protocol.
        // Thus, this rule just generates an error message for the users.

        throw ModelAnalysisException(
            """ |There may be no New-Expressions in methods, which create actors participating in the protocol specification.
                |
                |Offending statement: $stmtsHead
                |
                |Solution: Move all New-Expressions to the Main-Block and pass actor instances as constructor parameters.
            """.trimMargin()
        )
    }
}
