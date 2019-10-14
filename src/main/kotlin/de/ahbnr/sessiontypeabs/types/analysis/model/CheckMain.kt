package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.codegen.analysis.findCalls
import de.ahbnr.sessiontypeabs.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.codegen.analysis.parents
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.head
import de.ahbnr.sessiontypeabs.types.intersperse
import org.abs_models.frontend.ast.*

fun checkMainBlock(mainBlock: MainBlock, sessionType: AnalyzedGlobalType<CombinedDomain>, actorModelMapping: ActorModelMapping, verificationConfig: VerificationConfig) {
    val headType = sessionType.type.head

    if (headType !is GlobalType.Initialization) {
        throw ModelAnalysisException(
            message = "A session type must start with a call to the first actor, e. g. 0-f->g:m."
        )
    }

    else {
        val (callsToInitialActor, callsToOthers) =
            findCalls(mainBlock)
                .partition { actorModelMapping.findActorByType(it.calleeNoTransform.type) == headType.c && it.method == headType.m.value }

        when {
            callsToInitialActor.isEmpty() ->
                throw ModelAnalysisException("There is no initializing call corresponding to $headType in the model's main block.")
            callsToInitialActor.size > 1 ->
                throw ModelAnalysisException(
                    """
                        There may only be one initializing call to ${headType.c.value}::${headType.m} in the init block.
                        However, such a call appears multiple times at the following locations:
                        
${
                    callsToInitialActor
                        .map { "${it.fileName}: Column ${it.startColumn} Line ${it.startLine}" }
                        .intersperse("\n")
                    }
                    """.trimIndent().trimMargin()
                )
            else -> {
                val initialCall = callsToInitialActor.first()

                // The initial call may not be part of a conditional or repeating block
                if (initialCall.parents.let {
                            parents -> parents.any{ parent ->
                        parent is IfStmt
                            || parent is CaseStmt
                            || parent is WhileStmt
                            || parent is ForeachStmt
                            || parent is AnonymousFunctionDecl
                            || parent is TryCatchFinallyStmt && parent.catchsNoTransform.any { it in parents }
                    }
                    }) {
                    throw ModelAnalysisException(
                        """|The initial call to actor ${headType.c} in the main block,
                           |as required by the protocol, may not be nested in the following constructs:
                           |
                           |* If-Statements
                           |* Case-Statements
                           |* While-Loops
                           |* Foreach-Loops
                           |* Anonymous-Functions
                           |* Catch-Blocks
                           |""".trimMargin()
                    )
                }

                // No other call to a protocol participant is permitted, though all other usually communication relevant statements can be ignored, since the main block can
                // not be typed.
                if (verificationConfig.strictMain) {
                    val nonInitialCallToParticipant =
                        callsToOthers.find { actorModelMapping.findActorByType(it.calleeNoTransform.type) != null }
                    if (nonInitialCallToParticipant != null) {
                        throw ModelAnalysisException(
                            """
                                No other calls to participants of the Session Type are allowed.
                                This is violated by call $nonInitialCallToParticipant in file ${nonInitialCallToParticipant.fileName} at column ${nonInitialCallToParticipant.startColumn} and line ${nonInitialCallToParticipant.startLine}.
                            """.trimIndent().trimMargin()
                        )
                    }
                }

                checkForNew(
                    sessionType.postState.getParticipants().map(Class::value),
                    mainBlock
                )
            }
        }
    }
}


// FIXME: Does getClassName on NewExp yield the fully qualified name?
fun checkForNew(namesOfParticipants: Collection<String>, vararg stmts: Stmt) {
    val createdParticipants = mutableSetOf<String>()

    for (stmt in stmts) {
        stmt
            .findChildren<NewExp>()
            .filter { it.className in namesOfParticipants }
            .forEach { newExp ->
                if (newExp.className in createdParticipants) {
                    throw ModelAnalysisException(
                        """|The class ${newExp.className} is instantiated twice, but it participates in a session type and may thus only be instantiated once.
                           |
                           |Solution: Please remove all duplicate new-Expressions for the class.
                        """.trimMargin()
                    )
                }

                else {
                    val loop = newExp.parents.find{ it is WhileStmt }

                    if (loop != null) {
                        throw ModelAnalysisException(
                            """|The class ${newExp.className} might be instantiated multiple times,
                               |since there is a new expression for the class nested in a while-loop,
                               |but it participates in a session type and may thus only be instantiated once.
                               |
                               |Offending loop: Line ${loop.startLine}, Column ${loop.startColumn} @ file ${loop.fileName}.
                               |
                               |Solution: Please move the new expression outside the loop.
                            """.trimMargin()
                        )
                    }

                    else {
                        createdParticipants.add(newExp.className)
                    }
                }
            }
    }
}
