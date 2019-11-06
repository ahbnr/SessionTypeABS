package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.types.GlobalType

interface ConfigurableAnalysis<AnalysisT> {
    fun transfer(t: GlobalType): AnalysisT
    // FIXME: Change signature in thesis
    fun merge(rhs: AnalysisT, branchingContext: GlobalType.Branching): AnalysisT
    // FIXME: Change signature in thesis
    fun selfContained(preState: AnalysisT, context: GlobalType)
    fun closeScopes(preState: AnalysisT, context: GlobalType): AnalysisT
}