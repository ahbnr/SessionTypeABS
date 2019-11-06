package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.ConfigurableAnalysis
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.preprocessing.projection.project

/**
 * Checks a global session type for validity by executing it in the
 * given abstract analysis [AnalysisT]
 * (usually [de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.CombinedAnalysis]).
 *
 * For a definition of validity look up the documentation of the used analysis.
 * Usually this entails making sure that only active actors can invoke inactive actors etc.
 *
 * It produces an annotated version of the given type, containing the resulting
 * analysis instances as pre and poststates, since they contain useful
 * information for further processing (see [project], etc.).
 */
fun <AnalysisT> execute(initialState: AnalysisT, type: GlobalType): AnalyzedGlobalType<AnalysisT>
    where AnalysisT: ConfigurableAnalysis<AnalysisT>
=
    executeStep(initialState, type)
        .closeScopes(initialState, type)

private fun <AnalysisT> executeStep(
    state: AnalysisT,
    type: GlobalType
): AnalyzedGlobalType<AnalysisT>
    where AnalysisT: ConfigurableAnalysis<AnalysisT>
=
    when (type) {
        is GlobalType.Concatenation ->
            executeStep(state, type)
        is GlobalType.Repetition ->
            executeStep(state, type)
        is GlobalType.Branching ->
            executeStep(state, type)
        else ->
            if (type.isAtomic) {
                AnalyzedGlobalType.TerminalType(
                    type = type,
                    preState = state,
                    postState = state.transfer(type)
                )
            }

            else {
                throw RuntimeException(
                    """
                        Unknown session type. This should never happen and is a programmer error in Session Type ABS. 
                    """.trimIndent()
                )
            }
    }

private fun <AnalysisT> executeStep(
    state: AnalysisT,
    type: GlobalType.Concatenation
): AnalyzedGlobalType<AnalysisT>
    where AnalysisT: ConfigurableAnalysis<AnalysisT>
{
    val leftAnalyzedType = executeStep(state, type.lhs)
    val rightAnalyzedType = executeStep(leftAnalyzedType.postState, type.rhs)

    return AnalyzedGlobalType.ConcatenationType(
        type = type,
        preState = state,
        postState = rightAnalyzedType.postState,
        leftAnalyzedType = leftAnalyzedType,
        rightAnalyzedType = rightAnalyzedType
    )
}

private fun <AnalysisT> executeStep(
    state: AnalysisT,
    type: GlobalType.Repetition
): AnalyzedGlobalType<AnalysisT>
    where AnalysisT: ConfigurableAnalysis<AnalysisT>
{
    val innerAnalyzedType =
        executeStep(state, type.repeatedType)
            .closeScopes(state, type.repeatedType)

    innerAnalyzedType
        .postState
        .selfContained(state, type.repeatedType)

    return AnalyzedGlobalType.RepetitionType(
        type = type,
        preState = state,
        postState = innerAnalyzedType.postState,
        analyzedRepeatedType = innerAnalyzedType
    )
}

private fun <AnalysisT> executeStep(
    state: AnalysisT,
    type: GlobalType.Branching
): AnalyzedGlobalType<AnalysisT>
    where AnalysisT: ConfigurableAnalysis<AnalysisT>
{
    val analyzedBranches = type
        .branches
        .map {
            executeStep(state, it)
                .closeScopes(state, it)
        }

    val postState = analyzedBranches
        .fold(
            analyzedBranches.firstOrNull()?.postState ?: state,
            { acc, next ->
                acc.merge(next.postState, type)
            }
        )

    return AnalyzedGlobalType.BranchingType(
        type = type,
        preState = state,
        postState = postState,
        analyzedBranches = analyzedBranches
    )
}

