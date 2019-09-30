package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.*
import org.abs_models.frontend.ast.PureExp

private fun areFuturesEquivalent(introducedEquivalences: Map<Future, Future>, lhs: Future, rhs: Future) =
    lhs == rhs || introducedEquivalences[lhs] == rhs

fun futureEquivalent(lhs: MethodLocalType?, rhs: MethodLocalType?, introducedEquivalences: Map<Future, Future> = emptyMap()): Boolean =
    if (lhs != null && rhs != null) {
        val lHead = lhs.head
        val lTail = lhs.tail

        val rHead = rhs.head
        val rTail = rhs.tail

        when (lHead) {
            is MethodLocalType.Sending ->
                rHead is MethodLocalType.Sending &&
                    lHead.m == rHead.m &&
                    lHead.receiver == rHead.receiver &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences + (lHead.f to rHead.f)
                    )
            is MethodLocalType.Resolution ->
                rHead is MethodLocalType.Resolution &&
                    lHead.constructor == rHead.constructor &&
                    areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )
            is MethodLocalType.Fetching ->
                rHead is MethodLocalType.Fetching &&
                    lHead.constructor == rHead.constructor &&
                    areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )
            is MethodLocalType.Suspension ->
                rHead is MethodLocalType.Suspension &&
                    areFuturesEquivalent(introducedEquivalences, lHead.suspendedFuture, rHead.suspendedFuture) &&
                    areFuturesEquivalent(introducedEquivalences, lHead.awaitedFuture, rHead.awaitedFuture) &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )
            is MethodLocalType.Skip ->
                rHead is MethodLocalType.Skip &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )
            is MethodLocalType.Termination ->
                rHead is MethodLocalType.Termination &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )
            is MethodLocalType.Repetition ->
                rHead is MethodLocalType.Repetition &&
                    futureEquivalent(
                        lHead.repeatedType,
                        rHead.repeatedType,
                        introducedEquivalences
                    ) &&
                    futureEquivalent(
                        lTail,
                        rTail,
                        introducedEquivalences
                    )

            is MethodLocalType.Choice ->
                rHead is MethodLocalType.Choice &&
                    lHead.choices.all {lChoice ->
                        rHead.choices.any{rChoice ->
                            futureEquivalent(
                                lChoice concat lTail,
                                rChoice concat rTail,
                                introducedEquivalences
                            )
                        }
                    } &&
                    rHead.choices.all {rChoice ->
                        lHead.choices.any{lChoice ->
                            futureEquivalent(
                                lChoice concat lTail,
                                rChoice concat rTail,
                                introducedEquivalences
                            )
                        }
                    }
            is MethodLocalType.Offer ->
                rHead is MethodLocalType.Offer &&
                areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
                lHead.branches.keys == rHead.branches.keys &&
                lHead.branches.all { (lLabel, lBranch) ->
                    futureEquivalent(lBranch, rHead.branches[lLabel], introducedEquivalences)
                }
            is MethodLocalType.Concatenation ->
                throw RuntimeException("When applying the head function on a session type, this should never result in a concatenation type. It happened anyway, thus this is an error in the programming.")
        }
    }

    else {
        lhs == null && rhs == null
    }


//// FIXME: This overload is never used and can probably be removed
//fun futureEquivalent(lhs: LocalType?, rhs: LocalType?, introducedEquivalences: Map<Future, Future> = emptyMap()): Boolean =
//    if (lhs != null && rhs != null) {
//        val lHead = lhs.head
//        val lTail = lhs.tail
//
//        val rHead = rhs.head
//        val rTail = rhs.tail
//
//        when (lHead) {
//            is LocalType.Initialization ->
//                rHead is LocalType.Initialization &&
//                lHead.m == rHead.m &&
//                postConditionsBothPresentOrAbsent(lHead.postCondition, rHead.postCondition) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences + (lHead.f to rHead.f)
//                )
//            is LocalType.Sending ->
//                rHead is LocalType.Sending &&
//                lHead.m == rHead.m &&
//                lHead.receiver == rHead.receiver &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences + (lHead.f to rHead.f)
//                )
//            is LocalType.Receiving ->
//                rHead is LocalType.Receiving &&
//                lHead.m == rHead.m &&
//                lHead.sender == rHead.sender &&
//                postConditionsBothPresentOrAbsent(lHead.postCondition, rHead.postCondition) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences + (lHead.f to rHead.f)
//                )
//            is LocalType.Resolution ->
//                rHead is LocalType.Resolution &&
//                lHead.constructor == rHead.constructor &&
//                areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Fetching ->
//                rHead is LocalType.Fetching &&
//                lHead.constructor == rHead.constructor &&
//                areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Suspension ->
//                rHead is LocalType.Suspension &&
//                areFuturesEquivalent(introducedEquivalences, lHead.suspendedFuture, rHead.suspendedFuture) &&
//                areFuturesEquivalent(introducedEquivalences, lHead.awaitedFuture, rHead.awaitedFuture) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Reactivation ->
//                rHead is LocalType.Reactivation &&
//                areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Skip ->
//                rHead is LocalType.Skip &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Termination ->
//                rHead is LocalType.Termination &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//            is LocalType.Repetition ->
//                rHead is LocalType.Repetition &&
//                futureEquivalent(
//                    lHead.repeatedType,
//                    rHead.repeatedType,
//                    introducedEquivalences
//                ) &&
//                futureEquivalent(
//                    lTail,
//                    rTail,
//                    introducedEquivalences
//                )
//
//            is LocalType.Choice ->
//                rHead is LocalType.Choice &&
//                lHead.choices.all {lChoice ->
//                    rHead.choices.any{rChoice ->
//                        futureEquivalent(
//                            lChoice concat lTail,
//                            rChoice concat rTail,
//                            introducedEquivalences
//                        )
//                    }
//                } &&
//                rHead.choices.all {rChoice ->
//                    lHead.choices.any{lChoice ->
//                        futureEquivalent(
//                            lChoice concat lTail,
//                            rChoice concat rTail,
//                            introducedEquivalences
//                        )
//                    }
//                }
//            is LocalType.Offer ->
//                rHead is LocalType.Offer &&
//                areFuturesEquivalent(introducedEquivalences, lHead.f, rHead.f) &&
//                lHead.branches.all {lBranch ->
//                    rHead.branches.any{rBranch ->
//                        futureEquivalent(
//                            lBranch concat lTail,
//                            rBranch concat rTail,
//                            introducedEquivalences
//                        )
//                    }
//                } &&
//                rHead.branches.all {rBranch ->
//                    lHead.branches.any{lBranch ->
//                        futureEquivalent(
//                            lBranch concat lTail,
//                            rBranch concat rTail,
//                            introducedEquivalences
//                        )
//                    }
//                }
//            is LocalType.Concatenation ->
//                throw RuntimeException("When applying the head function on a session type, this should never result in a concatenation type. It happened anyway, thus this is an error in the programming.")
//        }
//    }
//
//    else {
//        lhs == null && rhs == null
//    }

// FIXME: If post conditions are present, they are not compared
//private fun postConditionsBothPresentOrAbsent(lhs: PureExp?, rhs: PureExp?) =
//    lhs == null && rhs == null ||
//        lhs != null && rhs != null

