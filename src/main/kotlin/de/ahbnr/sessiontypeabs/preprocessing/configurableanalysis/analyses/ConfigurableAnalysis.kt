package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.types.GlobalType

interface ConfigurableAnalysis<AnalysisT> {
    fun transfer(t: GlobalType): AnalysisT
    fun merge(rhs: AnalysisT, branchingContext: GlobalType.Branching): AnalysisT
    fun selfContained(preState: AnalysisT, context: GlobalType)
    fun closeScopes(preState: AnalysisT, context: GlobalType): AnalysisT
}