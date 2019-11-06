package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.throwNotAtomic
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType

data class ParticipantsAnalysis(
    val participants: Set<Class> = emptySet()
): ConfigurableAnalysis<ParticipantsAnalysis> {
    override fun transfer(t: GlobalType): ParticipantsAnalysis {
        val actors = when (t) {
            is GlobalType.Initialization -> setOf(t.c)
            is GlobalType.Resolution -> setOf(t.c)
            is GlobalType.Fetching -> setOf(t.c)
            is GlobalType.Release -> setOf(t.c)
            is GlobalType.Interaction -> setOf(t.caller, t.callee)
            is GlobalType.Skip -> emptySet()
            else -> throwNotAtomic(t)
        }

        return this.copy(
            participants = participants union actors
        )
    }

    override fun merge(rhs: ParticipantsAnalysis, branchingContext: GlobalType.Branching) =
        this.copy(
            participants = this.participants union rhs.participants
        )

    override fun selfContained(preState: ParticipantsAnalysis, context: GlobalType) { } // No checks in this analysis

    override fun closeScopes(preState: ParticipantsAnalysis, context: GlobalType) =
        this.copy()

    fun getParticipants(): Collection<Class> =
        this.participants
}