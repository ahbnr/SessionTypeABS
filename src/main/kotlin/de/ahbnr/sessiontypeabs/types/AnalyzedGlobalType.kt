package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.ConfigurableAnalysis

/**
 * Wraps global session types together with their abstract execution state
 * produced by [executeStep], which provides additional information useful
 * for validation and projection (see [project]) of a global session type.
 */
sealed class AnalyzedGlobalType<StateT>(
    open val type: GlobalType,
    open val preState: StateT,
    open val postState: StateT
)
    where StateT: ConfigurableAnalysis<StateT>
{
    data class TerminalType<StateT>(
        override val type: GlobalType,
        override val preState: StateT,
        override val postState: StateT
    ): AnalyzedGlobalType<StateT>(type, preState, postState)
        where StateT: ConfigurableAnalysis<StateT>
    {
        override fun closeScopes(preState: StateT, context: GlobalType?) =
            this.copy(
                postState = postState.closeScopes(preState, context ?: type)
            )
    }

    data class ConcatenationType<StateT>(
        override val type: GlobalType,
        override val preState: StateT,
        override val postState: StateT,
        val leftAnalyzedType: AnalyzedGlobalType<StateT>,
        val rightAnalyzedType: AnalyzedGlobalType<StateT>
    ): AnalyzedGlobalType<StateT>(type, preState, postState)
        where StateT: ConfigurableAnalysis<StateT>
    {
        override fun closeScopes(preState: StateT, context: GlobalType?): AnalyzedGlobalType<StateT> {
            return this.copy(
                postState = postState.closeScopes(preState, context ?: type)
            )
        }
    }


    data class BranchingType<StateT>(
        override val type: GlobalType,
        override val preState: StateT,
        override val postState: StateT,
        val analyzedBranches: List<AnalyzedGlobalType<StateT>>
    ): AnalyzedGlobalType<StateT>(type, preState, postState)
        where StateT: ConfigurableAnalysis<StateT>
    {
        fun getCastedType() = type as GlobalType.Branching

        override fun closeScopes(preState: StateT, context: GlobalType?): AnalyzedGlobalType<StateT> {
            return this.copy(
                postState = postState.closeScopes(preState, context ?: type)
            )
        }
    }

    data class RepetitionType<StateT>(
        override val type: GlobalType,
        override val preState: StateT,
        override val postState: StateT,
        val analyzedRepeatedType: AnalyzedGlobalType<StateT>
    ): AnalyzedGlobalType<StateT>(type, preState, postState)
        where StateT: ConfigurableAnalysis<StateT> {

        override fun closeScopes(preState: StateT, context: GlobalType?): AnalyzedGlobalType<StateT> {
            return this.copy(
                postState = postState.closeScopes(preState, context ?: type)
            )
        }
    }

    fun <ReturnT> accept(visitor: AnalyzedGlobalTypeVisitor<StateT, ReturnT>): ReturnT =
        when (this) {
            is TerminalType -> visitor.visit(this)
            is ConcatenationType -> visitor.visit(this)
            is BranchingType -> visitor.visit(this)
            is RepetitionType -> visitor.visit(this)
        }

    abstract fun closeScopes(preState: StateT, context: GlobalType? = null): AnalyzedGlobalType<StateT>
}

interface AnalyzedGlobalTypeVisitor<StateT, ReturnT>
    where StateT: ConfigurableAnalysis<StateT>
{
    fun visit(type: AnalyzedGlobalType.TerminalType<StateT>): ReturnT
    fun visit(type: AnalyzedGlobalType.ConcatenationType<StateT>): ReturnT
    fun visit(type: AnalyzedGlobalType.BranchingType<StateT>): ReturnT
    fun visit(type: AnalyzedGlobalType.RepetitionType<StateT>): ReturnT
}