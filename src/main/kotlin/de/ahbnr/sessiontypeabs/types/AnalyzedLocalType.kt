package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis

/**
 * Wraps local session types together with information about active and
 * used futures, derived from AnalyzedGlobalType during projection.
 * It provides additional information useful for method projection
 * (see [project]) of a local session type.
 */
sealed class AnalyzedLocalType(
    open val type: LocalType,
    open val preState: CombinedAnalysis,
    open val postState: CombinedAnalysis
) {
    data class TerminalType(
        override val type: LocalType,
        override val preState: CombinedAnalysis,
        override val postState: CombinedAnalysis
    ): AnalyzedLocalType(type, preState, postState)

    data class ConcatenationType(
        override val type: LocalType,
        override val preState: CombinedAnalysis,
        override val postState: CombinedAnalysis,
        val leftAnalyzedType: AnalyzedLocalType,
        val rightAnalzedType: AnalyzedLocalType
    ): AnalyzedLocalType(type, preState, postState)

    data class OfferType(
        override val type: LocalType,
        override val preState: CombinedAnalysis,
        override val postState: CombinedAnalysis,
        val analyzedBranches: List<AnalyzedLocalType>
    ): AnalyzedLocalType(type, preState, postState) {
        fun getCastedType() = type as LocalType.Offer
    }

    data class ChoiceType(
        override val type: LocalType,
        override val preState: CombinedAnalysis,
        override val postState: CombinedAnalysis,
        val analyzedChoices: List<AnalyzedLocalType>
    ): AnalyzedLocalType(type, preState, postState) {
        fun getCastedType() = type as LocalType.Choice
    }

    data class RepetitionType(
        override val type: LocalType,
        override val preState: CombinedAnalysis,
        override val postState: CombinedAnalysis,
        val analyzedRepeatedType: AnalyzedLocalType
    ): AnalyzedLocalType(type, preState, postState)

    fun <ReturnT> accept(visitor: AnalyzedLocalTypeVisitor<ReturnT>): ReturnT =
        when (this) {
            is TerminalType -> visitor.visit(this)
            is ConcatenationType -> visitor.visit(this)
            is OfferType -> visitor.visit(this)
            is ChoiceType -> visitor.visit(this)
            is RepetitionType -> visitor.visit(this)
        }
}

interface AnalyzedLocalTypeVisitor<ReturnT> {
    fun visit(type: AnalyzedLocalType.TerminalType): ReturnT
    fun visit(type: AnalyzedLocalType.ConcatenationType): ReturnT
    fun visit(type: AnalyzedLocalType.OfferType): ReturnT
    fun visit(type: AnalyzedLocalType.ChoiceType): ReturnT
    fun visit(type: AnalyzedLocalType.RepetitionType): ReturnT
}