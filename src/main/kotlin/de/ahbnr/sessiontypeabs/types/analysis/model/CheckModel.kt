package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.compiler.TypeBuild
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import org.abs_models.frontend.ast.*

fun checkModel(model: Model, typeBuild: TypeBuild) {
    // well-formed, since input type has already been analyzed

    val sessionType = typeBuild.analyzedProtocol
    val objectTypes = typeBuild.localTypes

    val classDecls = model.decls.filterIsInstance<ClassDecl>().toSet()
    val actorClassesMapping = ActorModelMapping(classDecls, sessionType)

    checkMainBlock(model.mainBlock, sessionType, actorClassesMapping)
    checkClasses(sessionType, objectTypes, actorClassesMapping)
}


//data class StmtRule(
//    val name: String,
//    val guard: (List<Stmt>, LocalType?, ActorModelMapping) -> Boolean,
//    val application: (List<Stmt>, LocalType?, ActorModelMapping) -> Unit
//)
//
//data class ExpRule(
//    val name: String,
//    val guard: (List<EffExp>, LocalType?, ActorModelMapping) -> Boolean,
//    val application: (List<EffExp>, LocalType?, ActorModelMapping) -> LocalType?
//)

//val sRuleSkip = StmtRule(
//    name = "sRuleSkip",
//
//    guard = { _, methodType, _ ->
//        methodType != null &&
//            methodType.head is LocalType.Skip
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        val remainingType = methodType!!.tail
//
//        //if (remainingType == null) {
//        //    throw ModelAnalysisException("A projected method type must end with a Put statement. Reaching the syntactical end of the type without encountering it should be impossible, but happened. Thus, this is a programmer error.")
//        //}
//
//        checkStmts(stmts, remainingType, actorModelMapping)
//    }
//)
//
//val sRuleMethodEnd = StmtRule(
//    name = "sRuleMethodEnd",
//
//    guard = { stmts, methodType, _ ->
//        methodType != null &&
//            stmts.isEmpty() &&
//            methodType.head is LocalType.Resolution
//    },
//
//    application = { stmts, methodType, _ ->
//        if (methodType!!.tail != null) {
//            throw RuntimeException("Method Session Type continues after a resolution type. This should not be possible after projection and is therefore a programming error in Session Type ABS.")
//        }
//
//        // axiom rule, no further recursion
//    }
//)
//
//val sRuleMethodEndFail = StmtRule(
//    name = "sRuleMethodEndFail",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isEmpty() &&
//            methodType.head !is LocalType.Resolution
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        // TODO more context information, e. g. what method
//        throw ModelAnalysisException(
//            """
//                |Method ended but no resolving type has been reached. Instead, the type encodes additional behavior.
//                |
//                |Specified additional behavior:
//                |${methodType!!}
//            """.trimMargin()
//        )
//    }
//)
//
//val sRuleReturn = StmtRule(
//    name = "sRuleReturn",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            stmts.head is ReturnStmt &&
//            methodType.head is LocalType.Resolution
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        if (stmts.size > 1) {
//            throw ModelAnalysisException("No further statements are allowed after a return statement.") // should be enforced by ABS compiler already
//        }
//
//        if (methodType!!.tail != null) {
//            throw RuntimeException("Method Session Type continues after a resolution type. This should not be possible after projection and is therefore a programming error in Session Type ABS.")
//        }
//
//        // axiom rule, no further recursion
//    }
//)
//
//val sRuleReturnFail = StmtRule(
//    name = "sRuleReturnFail",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            stmts.head is ReturnStmt &&
//            methodType.head !is LocalType.Resolution
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        throw ModelAnalysisException("Method ended with return statement, but no resolving type has been reached yet. Instead, the type encodes additional behavior.")
//    }
//)
//
//val sRuleAwait = StmtRule(
//    name = "sRuleAwait",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            methodType.head is LocalType.Suspension &&
//            stmts.isNotEmpty() &&
//            stmts.head is AwaitStmt
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        checkStmts(stmts.tail, methodType!!.tail, actorModelMapping)
//    }
//)
//
//val sRuleAwaitFail = StmtRule(
//    name = "sRuleAwaitFail",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            methodType.head !is LocalType.Suspension &&
//            stmts.isNotEmpty() &&
//            stmts.head is AwaitStmt
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        throw ModelAnalysisException(
//            """
//                |Method contains Await-Statement, which is not encoded in its session type.
//                |
//                |Offending statement: ${stmts.head}
//                |Relevant part of session type:
//                |${methodType!!}
//            """.trimMargin()
//        )
//    }
//)
//
//val sRuleBlock = StmtRule(
//    name = "sRuleBlock",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            stmts.head is Block
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        checkStmts(
//            (stmts.head as Block).stmtsNoTransform.toList() + stmts.tail,
//            methodType!!,
//            actorModelMapping
//        )
//    }
//)
//
//val sRuleLoop = StmtRule(
//    name = "sRuleLoop",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            methodType.head is LocalType.Repetition &&
//            stmts.isNotEmpty() &&
//            stmts.head.let { stmt ->
//                (stmt is WhileStmt || stmt is ForeachStmt) &&
//                    !areStmtAndSubBlocksCommunicationInert(
//                        stmt,
//                        actorModelMapping
//                    )
//            }
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        val loopBody =
//            when (val stmt = stmts.head) {
//                is WhileStmt -> stmt.bodyNoTransform.stmtsNoTransform.toList()
//                else -> (stmt as ForeachStmt).bodyNoTransform.stmtsNoTransform.toList()
//            }
//
//        checkStmts(
//            loopBody,
//            (methodType!!.head as LocalType.Repetition).repeatedType,
//            actorModelMapping
//        )
//        checkStmts(stmts.tail, methodType!!.tail, actorModelMapping)
//    }
//)
//
//val sRuleLoopFail = StmtRule(
//    name = "sRuleLoopFail",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            methodType.head !is LocalType.Repetition &&
//            stmts.isNotEmpty() &&
//            stmts.head.let { stmt ->
//                (stmt is WhileStmt || stmt is ForeachStmt) &&
//                    !areStmtAndSubBlocksCommunicationInert(
//                        stmt,
//                        actorModelMapping
//                    )
//            }
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        throw ModelAnalysisException("The method contains a loop with communication relevant statements inside, but it is not encoded in the session type, at least not at the right position.")
//    }
//)
//
//val sRuleIgnore = StmtRule(
//    name = "sRuleIgnore",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            areStmtAndSubBlocksCommunicationInert(
//                stmts.head,
//                actorModelMapping
//            )
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        checkStmts(stmts.tail, methodType, actorModelMapping)
//    }
//)
//
//// FIXME disallow all async calls to non participants, or keep track of the created futures and ensure, that we do not count get expressions etc. on those futures and disallow control release on those futures etc.
//val eRuleAsyncCall = ExpRule(
//    name = "eRuleAsyncCall",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head.let { exp ->
//                exp is AsyncCall &&
//                    methodType.head.let { sendingType ->
//                        sendingType is LocalType.Sending &&
//                            exp.methodSig.name == sendingType.m.value &&
//                            actorModelMapping.findActorByType(exp.calleeNoTransform.type) == sendingType.receiver
//                    }
//            }
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        checkExpressions(exps.tail, methodType!!.tail, actorModelMapping)
//    }
//)
//
//val eRuleAsyncCallFail = ExpRule(
//    name = "eRuleAsyncCallFail",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head.let { exp ->
//                exp is AsyncCall &&
//                    !isCommunicationInert(exp, actorModelMapping) &&
//                    methodType.head.let { sendingType ->
//                        sendingType !is LocalType.Sending ||
//                            exp.methodSig.name != sendingType.m.value ||
//                            actorModelMapping.findActorByType(exp.calleeNoTransform.type) != sendingType.receiver
//                    }
//            }
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        val typeHead = methodType!!.head
//        val exp = exps.head as AsyncCall
//
//        when {
//            typeHead !is LocalType.Sending ->
//                throw ModelAnalysisException(
//                    """
//                        |The method makes an asynchronous call, relevant to the protocol participants, but the protocol does not expect a call at this point.
//                        |
//                        |Offending call: $exp
//                        |Session type part:
//                        |$methodType
//                    """.trimMargin()
//                )
//            exp.methodSig.name != typeHead.m.value ->
//                throw ModelAnalysisException("The method makes an asynchronous call, relevant to the protocol participants, but the called method does not match the one from the session type at this point in the protocol.")
//            actorModelMapping.findActorByType(exp.calleeNoTransform.type) != typeHead.receiver ->
//                throw ModelAnalysisException("The method makes an asynchronous call, relevant to the protocol participants, but the called actor does not match the one from the session type at this point in the protocol.")
//            else ->
//                throw ModelAnalysisException("Typing an asynchronous call failed for an unknown reason. This is likely a programmer error.")
//        }
//    }
//)
//
//val eAwaitAsyncCall = ExpRule(
//    name = "eAwaitAsyncCall",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head.let { exp ->
//                exp is AwaitAsyncCall &&
//                    methodType.head.let { sendingType ->
//                        sendingType is LocalType.Sending &&
//                            methodType.tail?.head?.let { it is LocalType.Suspension } ?: false && // It should also be possible to leave Skips between Sending and Suspension, but the projection should remove that
//                            exp.methodSig.name == sendingType.m.value &&
//                            actorModelMapping.findActorByType(exp.calleeNoTransform.type) == sendingType.receiver
//                    }
//            }
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        checkExpressions(
//            exps.tail,
//            methodType!!.tail?.tail,
//            actorModelMapping
//        )
//    }
//)
//
//val eRuleAwaitAsyncCall = ExpRule(
//    name = "eRuleAwaitAsyncCall",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head.let { exp ->
//                exp is AwaitAsyncCall &&
//                    methodType.head.let { sendingType ->
//                        sendingType is LocalType.Sending &&
//                            exp.methodSig.name == sendingType.m.value &&
//                            actorModelMapping.findActorByType(exp.calleeNoTransform.type) == sendingType.receiver &&
//                            methodType.tail?.head?.let { typeTail ->
//                                typeTail.head.let { it is LocalType.Suspension } && // It should also be possible to leave Skips between Sending and Fetching, but the projection should remove that
//                                    typeTail.tail?.head.let { it is LocalType.Fetching } ?: false
//                            } ?: false
//                    }
//            }
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        checkExpressions(
//            exps.tail,
//            methodType!!.tail?.tail?.tail,
//            actorModelMapping
//        )
//    }
//)
//
//val eRuleAwaitAsyncCallFail = ExpRule(
//    name = "eRuleAwaitAsyncCallFail",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head.let { exp ->
//                exp is AwaitAsyncCall && // we do not need to also check, whether it is communication inert, since AwaitExps always are
//                    methodType.head.let { sendingType ->
//                        sendingType !is LocalType.Sending ||
//                            exp.methodSig.name != sendingType.m.value ||
//                            actorModelMapping.findActorByType(exp.calleeNoTransform.type) != sendingType.receiver ||
//                            methodType.tail?.head?.let { typeTail ->
//                                typeTail.head?.let { it !is LocalType.Suspension } || // It should also be possible to leave Skips between Sending and Fetching, but the projection should remove that
//                                    typeTail.tail?.head?.let { it !is LocalType.Fetching } ?: true
//                            } ?: true
//                    }
//            }
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        val typeTail = methodType!!.tail
//
//        val maybeSendingType = methodType!!.head
//        val maybeSuspendingType = typeTail?.head
//        val maybeFetchingType = typeTail?.tail?.head
//
//        val exp = exps.head as SyncCall
//
//        when {
//            maybeSendingType !is LocalType.Sending ->
//                throw ModelAnalysisException("The method contains an await expression, but the protocol does not expect a call at this point.")
//            maybeSuspendingType !is LocalType.Suspension ->
//                throw ModelAnalysisException("The method contains an await expression, but the protocol does not contain a control release directly after the interaction.")
//            maybeFetchingType !is LocalType.Fetching ->
//                throw ModelAnalysisException("The method contains an await expression, but the protocol does not specify to read the call result after the interaction and control release.")
//            exp.methodSig.name != maybeSendingType.m.value ->
//                throw ModelAnalysisException("The method contains an await expression, relevant to the protocol participants, but the called method does not match the one from the session type at this point in the protocol.")
//            actorModelMapping.findActorByType(exp.calleeNoTransform.type) != maybeSendingType.receiver ->
//                throw ModelAnalysisException("The method contains an await expression, relevant to the protocol participants, but the called actor does not match the one from the session type at this point in the protocol.")
//            else ->
//                throw ModelAnalysisException("Typing an await expression failed for an unknown reason. This is likely a programmer error in Session Type ABS.")
//        }
//    }
//)
//
//// FIXME: Allow Sync Calls. The following two rules could implement them, but the semantics of session types dont support complete release of control of a COG
//
//// FIXME count new as a call to run(), count and restrict initializations of actors
//val eRuleNew = ExpRule(
//    name = "eRuleNew",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head is NewExp
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        checkExpressions(exps.tail, methodType, actorModelMapping)
//    }
//)
//
//val eRuleTypeEnd = ExpRule(
//    name = "eRuleTypeEnd",
//
//    // TODO: We might want to ensure that we are in a loop, since this is the only case this rule should be applied. Otherwise there is an error in the programming
//    guard = { exps, methodType, actorModelMapping ->
//        methodType == null &&
//            (exps.isEmpty() || exps.tail.all {
//                isCommunicationInert(
//                    it,
//                    actorModelMapping
//                )
//            })
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        methodType
//    }
//)
//
//val eRuleGet = ExpRule(
//    name = "eRuleGet",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            methodType.head is LocalType.Fetching &&
//            exps.isNotEmpty() &&
//            exps.head is GetExp
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        checkExpressions(exps.tail, methodType!!.tail, actorModelMapping)
//    }
//)
//
//val eRuleGetFail = ExpRule(
//    name = "eRuleGetFail",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isNotEmpty() &&
//            exps.head is GetExp &&
//            methodType.head !is LocalType.Fetching
//    },
//
//    application = { exps, methodType, actorModelMapping ->
//        throw ModelAnalysisException(
//            """
//                |The method contains a get expression, but the protocol does not expect a fetching action at this point.
//                |
//                |Offending expression: ${exps.head}
//                |Current session type part:
//                |${methodType!!.head}"
//            """.trimMargin()
//        )
//    }
//)
//
//val eRuleEmptyExps = ExpRule(
//    name = "eRuleEmptyExps",
//
//    guard = { exps, methodType, actorModelMapping ->
//        methodType != null &&
//            exps.isEmpty()
//    },
//
//    application = { exps, methodType, actorModelMapping -> methodType }
//)
//
//val sRuleIf = StmtRule(
//    name = "sRuleIf",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            stmts.head.let { stmt ->
//                stmt is IfStmt &&
//                    !areStmtAndSubBlocksCommunicationInert(
//                        stmt,
//                        actorModelMapping
//                    )
//            }
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        (stmts.head as IfStmt).let { ifStmt ->
//            checkStmts(
//                ifStmt.thenNoTransform.stmtsNoTransform.toList() + stmts.tail,
//                methodType,
//                actorModelMapping
//            )
//
//            when (val maybeElse = ifStmt.`else`) {
//                null -> checkStmts(
//                    stmts.tail,
//                    methodType,
//                    actorModelMapping
//                )
//                else -> checkStmts(
//                    maybeElse.stmtsNoTransform.toList() + stmts.tail,
//                    methodType,
//                    actorModelMapping
//                )
//            }
//        }
//    }
//)
//
//val sRuleTypeEnd = StmtRule(
//    name = "sRuleTypeEnd",
//
//    // TODO: We might want to ensure that we are in a loop, since this is the only case this rule should be applied. Otherwise there is an error in the programming
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType == null &&
//            (stmts.isEmpty() ||
//                areStmtAndSubBlocksCommunicationInert(
//                    stmts.head,
//                    actorModelMapping
//                ) &&
//                stmts.tail.all {
//                    isCommunicationInert(
//                        it,
//                        actorModelMapping
//                    )
//                }
//                )
//    },
//
//    application = { stmts, methodType, actorModelMapping -> }
//)
//
//val sRuleCase = StmtRule(
//    name = "sRuleCase",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            stmts.isNotEmpty() &&
//            stmts.head.let { stmt ->
//                stmt is CaseStmt &&
//                    !areStmtAndSubBlocksCommunicationInert(
//                        stmt,
//                        actorModelMapping
//                    )
//            }
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        (stmts.head as CaseStmt).let { caseStmt ->
//            caseStmt
//                .branchsNoTransform
//                .map { branch -> branch.rightNoTransform.stmts.toList() }
//                .forEach { branchStmts ->
//                    checkStmts(
//                        branchStmts + stmts.tail,
//                        methodType,
//                        actorModelMapping
//                    )
//                }
//        }
//    }
//)
//
//val sRuleChoiceNOffer = StmtRule(
//    name = "sRuleChoiceNOffer",
//
//    guard = { stmts, methodType, actorModelMapping ->
//        methodType != null &&
//            (methodType.head is LocalType.Choice || methodType.head is LocalType.Offer) &&
//            stmts.isNotEmpty() &&
//            stmts.head.let { stmt ->
//                stmt !is IfStmt &&
//                    stmt !is CaseStmt
//            }
//    },
//
//    application = { stmts, methodType, actorModelMapping ->
//        val typeHead = methodType!!.head
//        val branchTypes =
//            when (typeHead) {
//                is LocalType.Choice -> typeHead.choices
//                else -> (typeHead as LocalType.Offer).branches
//            }
//
//        val branchesExceptions = branchTypes // FIXME: Use return values instead of exceptions
//            .map { choice ->
//                try {
//                    checkStmts(stmts, choice, actorModelMapping)
//
//                    null
//                } catch (e: ModelAnalysisException) {
//                    e
//                }
//            }
//
//        if (branchesExceptions.any { it != null }) {
//            throw ModelAnalysisException(
//                "None of the control flow branches in file ${stmts.head.fileName} at column ${stmts.head.startColumn} at line ${stmts.head.startLine} match the choice type ${methodType.head}."
//            )
//        }
//    }
//)
//
//val stmtRules = listOf(
//    sRuleBlock,
//    sRuleAwait,
//    sRuleAwaitFail,
//    sRuleIf,
//    sRuleCase,
//    sRuleChoiceNOffer,
//    sRuleIgnore,
//    sRuleLoop,
//    sRuleLoopFail,
//    sRuleMethodEnd,
//    sRuleMethodEndFail,
//    sRuleReturn,
//    sRuleReturnFail,
//    sRuleSkip,
//    sRuleTypeEnd
//)

//val expRules = listOf(
//    eRuleAsyncCall,
//    eRuleAsyncCallFail,
//    eRuleAwaitAsyncCall,
//    eRuleAwaitAsyncCallFail,
//    eRuleGet,
//    eRuleGetFail,
//    eRuleEmptyExps,
//    eRuleNew,
//    eRuleTypeEnd
//)


//fun checkExpressions(exps: List<EffExp>, methodType: LocalType?, actorModelMapping: ActorModelMapping): LocalType? {
//    if (exps.isNotEmpty()) {
//        checkIfSupported(exps.head)
//    }
//
//    val applicableRules = expRules
//        .filter { // If we are confident, that the rules exclude each other, we could omit the next check and use find instead of filter
//            it.guard(exps, methodType, actorModelMapping)
//        }
//
//    return when {
//        applicableRules.isEmpty() -> throw ModelAnalysisException(
//            """
//                |Encountered an unknown source code / session type pattern.
//                |Either you are using an unsupported ABS feature, or this is an error in Session Type ABS.
//                |
//                |Offending expression: ${
//                    if (exps.isEmpty()) {
//                        "No specific expression, but the lack of an expression to analyze."
//                    }
//
//                    else {
//                        "${exps.head}"
//                    }
//                }
//                |Current session type part: ${
//                    if (methodType == null) {
//                        "No further behavior specified by session type at this point."
//                    }
//
//                    else {
//                        "${methodType.head}"
//                    }
//                }
//            """.trimMargin())
//        applicableRules.size > 1 -> throw ModelAnalysisException("More than one analysis rule is applicable. This should never happen and is an error in Session Type ABS.")
//        else -> applicableRules.head.application(exps, methodType, actorModelMapping)
//    }
//}


//fun areStmtAndSubBlocksCommunicationInert(stmt: Stmt, actorModelMapping: ActorModelMapping) =
//    when (stmt) {
//        is AwaitStmt, is ReturnStmt -> false // these are relevant to communication
//        // these have no sub blocks and their defining expression has already been checked before checking the statement as a whole
//        is VarDeclStmt, is AssignStmt, is ExpressionStmt -> true
//        // all others can carry no head conditions, we can check them as a whole
//        else -> isCommunicationInert(stmt, actorModelMapping)
//    }

//fun <T> isCommunicationInert(node: ASTNode<T>, actorModelMapping: ActorModelMapping): Boolean
//    where T : ASTNode<*> =
//       node.astChildrenNoTransform().all{
//           isCommunicationInert(
//               it,
//               actorModelMapping
//           )
//       }
//    && when (node) {
//        is Call -> actorModelMapping.findActorByType(node.calleeNoTransform.type) == null // TODO: Does not allow calling methods, which are not specified in the protocol. Do we want this?
//        is AwaitAsyncCall, is AwaitStmt, is ReturnStmt -> false
//        else -> true
//    }


//fun extractEffExps(exp: Exp): List<EffExp> =
//    when {
//        exp is EffExp ->
//            when (exp) {
//                is Call -> listOf(exp)
//                is GetExp -> listOf(exp)
//                is NewExp -> listOf(exp)
//                else -> throw ModelAnalysisException(
//                    """
//                        Found an EffExp not supported by Session Type ABS.
//                        Offending expression: $exp
//                    """.trimIndent().trimMargin()
//                )
//            }
//        exp.isPure -> emptyList()
//        else -> throw ModelAnalysisException(
//            """
//                Found an expression which is neither an EffExp nor pure. Thus, it is not supported by Session Type ABS.
//                Offending expression: $exp
//            """.trimIndent().trimMargin()
//        )
//    }
//
//fun extractEffExps(stmt: Stmt): List<EffExp> =
//// Deriving possible statements and effect expressions, as well as their evaluation order from Deliverable 1.2
//// and by examining the ABS compiler source code.
//    // Inner statements of blocks and their expressions are not considered
//    when (stmt) {
//        is Block -> emptyList() // since those 3 can carry expressions in their body, we handle them explicitly
//        is IfStmt -> emptyList()
//        is CaseStmt -> emptyList()
//        is WhileStmt -> emptyList()
//        is ForeachStmt -> emptyList()
//        is VarDeclStmt ->
//            when (val maybeInitExp = stmt.varDeclNoTransform.initExp) {
//                null -> emptyList()
//                else -> extractEffExps(maybeInitExp)
//            }
//        // Deliverable requires PureExp, but implementation seems to allow Exp
//        is AssignStmt -> extractEffExps(stmt.valueNoTransform)
//        // Deliverable requires PureExp, but implementation seems to allow Exp
//        is ReturnStmt -> extractEffExps(stmt.retExpNoTransform)
//        is ExpressionStmt -> extractEffExps(stmt.expNoTransform)
//        else ->
//            if (findEffExps(stmt).isNotEmpty()) {
//                throw ModelAnalysisException(
//                    """
//                        Encountered statement not supported by Session Type ABS which contains expressions with side-effects for which we can not determine their evaluation order.
//                        Thus your model is currently not supported.
//
//                        Offending statement: $stmt
//                    """.trimIndent().trimMargin()
//                )
//            }
//
//            else {
//                emptyList()
//            }
//    }
