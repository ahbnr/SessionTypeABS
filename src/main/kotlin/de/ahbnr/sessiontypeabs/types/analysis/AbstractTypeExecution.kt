package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Finalizable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Transferable
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException

/**
 * Checks a global session type for validity by executing it in the
 * given abstract domain [DomainT] (usually [de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain]).
 *
 * For a definition of validity look up the documentation of the used domain.
 * Usually this entails making sure, that only active actors can invoke inactive actors etc.
 *
 * It produces an annotated version of the given type, containing the resulting
 * domain instances as pre and poststates, since they contain useful
 * information for further processing (see [project], etc.).
 */
fun <DomainT> execute(preState: DomainT, type: GlobalType): AnalyzedGlobalType<DomainT>
    where
    DomainT: Mergeable<DomainT>,
    DomainT: Transferable<GlobalType, DomainT>,
    DomainT: Repeatable<DomainT>,
    DomainT: Finalizable<DomainT>
{
    return applyFinalization(executeStep(preState, type))
}


private fun <DomainT> executeStep(preState: DomainT, type: GlobalType)
    where
    DomainT: Mergeable<DomainT>,
    DomainT: Transferable<GlobalType, DomainT>,
    DomainT: Repeatable<DomainT>,
    DomainT: Finalizable<DomainT>
=  when (type) {
    is GlobalType.Concatenation -> executeStep(preState, type)
    is GlobalType.Branching -> executeStep(preState, type)
    is GlobalType.Repetition -> executeStep(preState, type)
    else -> {
        val postState = preState.transfer(type)

        AnalyzedGlobalType.TerminalType(
            type = type,
            preState = preState,
            postState = postState
        )
    }
}

private fun <T> executeStep(preState: T, type: GlobalType.Concatenation): AnalyzedGlobalType<T>
    where
        T: Mergeable<T>,
        T: Transferable<GlobalType, T>,
        T: Repeatable<T>,
        T: Finalizable<T>
{
    val leftAnalysis = executeStep(preState, type.lhs)
    val rightAnalysis = executeStep(leftAnalysis.postState, type.rhs)

    return AnalyzedGlobalType.ConcatenationType(
        type = type,
        preState = preState,
        postState = rightAnalysis.postState,
        leftAnalyzedType = leftAnalysis,
        rightAnalzedType = rightAnalysis
    )
}

private fun <T> executeStep(preState: T, type: GlobalType.Branching): AnalyzedGlobalType<T>
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>,
    T: Finalizable<T>
{
    val branchAnalysis = type
        .branches
        .map { branchT ->
            applyFinalization(executeStep(preState, branchT))
        }

    // Combine resulting states from all branches into one
    // possibly more abstract post state, which covers all of them.
    // See [Mergeable]
    val postState =
        when {
            branchAnalysis.isEmpty() -> preState
            else ->
                branchAnalysis
                    .map { analyzedBranch -> analyzedBranch.postState }
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

/**
 * Some notes on validating type repetition:
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

private fun <T> executeStep(preState: T, type: GlobalType.Repetition): AnalyzedGlobalType<T>
    where
    T: Mergeable<T>,
    T: Transferable<GlobalType, T>,
    T: Repeatable<T>,
    T: Finalizable<T>
{
    // We can weaken the self-containedness requirement, if the following holds:
    //  (1) An iteration can make a state always only more abstract or results in an equal state
    //  (2) All used lattices are finite
    //
    // OR other possibilities, see ASV
    //
    // In this case we could executeStep the loop until it stabilizes and merge all possible results.

    // Self-containedness requirement
    val iterationResult = applyFinalization(executeStep(preState, type.repeatedType))

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
        throw TransferException(
            type,
            "Repetition type is not self-contained. Errors: $errorDescriptions"
        ) // TODO replace with ExecutionException
    }
}

private fun <DomainT> applyFinalization(type: AnalyzedGlobalType<DomainT>)
    where DomainT: Finalizable<DomainT>
=
    when (type) {
        is AnalyzedGlobalType.ConcatenationType<DomainT> -> type.copy(
            postState = type.postState.finalizeScope(type.type)
        )
        is AnalyzedGlobalType.BranchingType<DomainT> -> type.copy(
            postState = type.postState.finalizeScope(type.type)
        )
        is AnalyzedGlobalType.RepetitionType<DomainT> -> type.copy(
            postState = type.postState.finalizeScope(type.type)
        )
        is AnalyzedGlobalType.TerminalType<DomainT> -> type.copy(
            postState = type.postState.finalizeScope(type.type)
        )
    }
