package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.GlobalType

/**
 * Recorded properties and the influence of repetition on them
 *
 * usedFutures:
 *   * future identifiers introduced within a loop are considered to be fresh in the next iteration
 *   * Open questions: Do we consider futures to be usable after a loop?
 *       Considering that they may be used in ABS after exiting a looping construct, yes!
 *
 * activeClasses:
 *   Cases:
 *     1) Class c with future f (c, f) was not active before entering the loop and is not active after leaving the
 *     loop. Trivially irrelevant.
 *
 *     2) (c, f) was not active before the loop, but is active after the loop.
 *
 *     Primary question: Could c being active in a repetition contradict the logic of the type?
 *
 *     Considering the subcases in which class activity is relevant:
 *       2.1) Releases: Would require being active already in the first iteration, irrelevant
 *       2.2) Resolution: Same
 *
 *     3) (c, f) was active before the loop, but is not active after the loop.
 *       Therefore the repeated type must contain Releases or Resolutions
 *
 *       A repetition could therefore only be valid iff the repeated type contains different branches
 *       and there exists a path without those Releases or Resolutions (We can not ensure that the correct path is
 *       taken using only type analysis, since branching types contain no conditions!)
 *
 *     4) (c, f) was active before the loop and is active after the loop. Trivially irrelevant.
 *
 * suspendedClasses:
 *   Cases:
 *     1) Class c is not suspended on future f (c,f) before and after the loop body. Trivially irrelevant.
 *     2) (c, f) not suspended before loop, but after loop body.
 *        Loop body must contain a Release. Similar to above 3), only correct if there exists a path without
 *        Release on f.
 *
 *     3) (c, f) suspended before loop, but not after loop.
 *        Loop body must contain a Resolution of f. Only valid iff there exists a path without resolution of f within
 *        the loop body.
 *     4) (c, f) suspended before and after loop body, trivially irrelevant
 *
 *
 *  How about states at the start and end of the Repetition?
 *      Start: s0 + States at end of loop body
 *          Primary question: Do we need to consider additional states for a third iteration?
 *
 *          We need to show, that application of the loop body is idempotent in regard to the abstract states.
 *          What changes can arise?
 *
 *          [Introduction of used futures] The number of futures being introduced by a type is static. Idempotent.
 *
 *          [Activation of futures] TODO
 *
 *          [Resolution of futures] TODO
 *
 *          [Suspension on futures] TODO
 *
 *  Solution: Heavier overapproximation: Any (T) values instead of sets of possible states
 *
 */

/*
private fun <T> execute(state: T, type: GlobalType.Concatenation): T
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>
{
    val s1 = execute(state, type.lhs)
    val s2 = execute(s1, type.rhs)

    return s2
}

private fun <T> execute(state: T, type: GlobalType.Branching): T
    where
        T: Mergeable<T>,
        T: Transferable<GlobalType, T>,
        T: Repeatable<T>
    = when {
        type.branches.isEmpty() -> state
        else ->
            type.branches
                .map{t -> execute(state, t)}
                .reduce {
                    acc, elem -> acc merge elem
                }
    }

private fun <T> execute(state: T, type: GlobalType.Repetition): T
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>
{
    // We can weaken the self-containedness requirement, if the following holds:
    //  (1) An iteration can make a state always only more abstract or results in an equal state
    //  (2) All used lattices are finite
    //
    // OR other possibilities, see ASV
    //
    // In this case we could execute the loop until it stabilizes and merge all possible results.

    // Self-containedness requirement
    val iterationResult = execute(state, type.repeatedType)

    val errorDescriptions = mutableListOf<String>()
    if (iterationResult.loopContained(state, errorDescriptions)) {
        return state
    }

    else {
        throw TransferException(type, "Repetition type is not self-contained. Errors: ${errorDescriptions}") // TODO replace with ExecutionException
    }
}

fun <T> execute(state: T, type: GlobalType)
    where
        T: Mergeable<T>,
        T: Transferable<GlobalType, T>,
        T: Repeatable<T>
= when (type) {
    is GlobalType.Concatenation -> execute(state, type)
    is GlobalType.Branching -> execute(state, type)
    is GlobalType.Repetition -> execute(state, type)
    else -> state.transfer(type)
}*/

fun <T> analyze(preState: T, type: GlobalType.Concatenation): AnalyzedGlobalType<T>
    where
        T: Mergeable<T>,
        T: Transferable<GlobalType, T>,
        T: Repeatable<T>
{
    val leftAnalysis = analyze(preState, type.lhs)
    val rightAnalysis = analyze(leftAnalysis.postState, type.rhs)

    return AnalyzedGlobalType.ConcatenationType(
        type = type,
        preState = preState,
        postState = rightAnalysis.postState,
        leftAnalyzedType = leftAnalysis,
        rightAnalzedType = rightAnalysis
    )
}

fun <T> analyze(preState: T, type: GlobalType.Branching): AnalyzedGlobalType<T>
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>
{
    val branchAnalysis = type
        .branches
        .map { branchT -> analyze(preState, branchT) }

    val postState =
        when {
            branchAnalysis.isEmpty() -> preState
            else ->
                branchAnalysis
                    .map { analzedBranch -> analzedBranch.postState }
                    .reduce {
                        acc, elem -> acc merge elem
                    }
        }

    return AnalyzedGlobalType.BranchingType(
        type = type,
        preState = preState,
        postState = postState,
        analyzedBranches = branchAnalysis
    )
}

fun <T> analyze(preState: T, type: GlobalType.Repetition): AnalyzedGlobalType<T>
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>
{
    // We can weaken the self-containedness requirement, if the following holds:
    //  (1) An iteration can make a state always only more abstract or results in an equal state
    //  (2) All used lattices are finite
    //
    // OR other possibilities, see ASV
    //
    // In this case we could execute the loop until it stabilizes and merge all possible results.

    // Self-containedness requirement
    val iterationResult = analyze(preState, type.repeatedType)

    val errorDescriptions = mutableListOf<String>()
    if (iterationResult.postState.loopContained(preState, errorDescriptions)) {
        return AnalyzedGlobalType.RepetitionType(
            type = type,
            preState = preState,
            postState = iterationResult.postState, // Need to use poststate because of new futures etc. TODO: Maybe introduce domain specific function to determine poststate
            analyzedRepeatedType = iterationResult
        )
    }

    else {
        throw TransferException(type, "Repetition type is not self-contained. Errors: ${errorDescriptions}") // TODO replace with ExecutionException
    }
}

fun <T> analyze(preState: T, type: GlobalType)
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>
=  when (type) {
    is GlobalType.Concatenation -> analyze(preState, type)
    is GlobalType.Branching -> analyze(preState, type)
    is GlobalType.Repetition -> analyze(preState, type)
    else -> {
        val postState = preState.transfer(type)

        AnalyzedGlobalType.TerminalType(
            type = type,
            preState = preState,
            postState = postState
        )
    }
}

