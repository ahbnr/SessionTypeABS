package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.MethodBinding
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType

data class CombinedAnalysis(
    val participantsAnalysis: ParticipantsAnalysis = ParticipantsAnalysis(),
    val futureFreshnessAnalysis: FutureFreshnessAnalysis = FutureFreshnessAnalysis(),
    val actorActivityAnalysis: ActorActivityAnalysis = ActorActivityAnalysis(),
    val resolutionAnalysis: ResolutionAnalysis = ResolutionAnalysis()
): ConfigurableAnalysis<CombinedAnalysis> {
    override fun transfer(t: GlobalType) =
        this.copy(
            participantsAnalysis = this.participantsAnalysis.transfer(t),
            futureFreshnessAnalysis = this.futureFreshnessAnalysis.transfer(t),
            actorActivityAnalysis = this.actorActivityAnalysis.transfer(t),
            resolutionAnalysis = this.resolutionAnalysis.transfer(t)
        )

    override fun merge(rhs: CombinedAnalysis, branchingContext: GlobalType.Branching) =
        this.copy(
            participantsAnalysis = this.participantsAnalysis.merge(rhs.participantsAnalysis, branchingContext),
            futureFreshnessAnalysis = this.futureFreshnessAnalysis.merge(rhs.futureFreshnessAnalysis, branchingContext),
            actorActivityAnalysis = this.actorActivityAnalysis.merge(rhs.actorActivityAnalysis, branchingContext),
            resolutionAnalysis = this.resolutionAnalysis.merge(rhs.resolutionAnalysis, branchingContext)
        )

    override fun selfContained(preState: CombinedAnalysis, context: GlobalType) {
        participantsAnalysis.selfContained(preState.participantsAnalysis, context)
        futureFreshnessAnalysis.selfContained(preState.futureFreshnessAnalysis, context)
        actorActivityAnalysis.selfContained(preState.actorActivityAnalysis, context)
        resolutionAnalysis.selfContained(preState.resolutionAnalysis, context)
    }

    override fun closeScopes(preState: CombinedAnalysis, context: GlobalType) =
        this.copy(
            participantsAnalysis = participantsAnalysis.closeScopes(preState.participantsAnalysis, context),
            futureFreshnessAnalysis = futureFreshnessAnalysis.closeScopes(preState.futureFreshnessAnalysis, context),
            actorActivityAnalysis = actorActivityAnalysis.closeScopes(preState.actorActivityAnalysis, context),
            resolutionAnalysis = resolutionAnalysis.closeScopes(preState.resolutionAnalysis, context)
        )

    // Getters for Projection
    fun getParticipants(): Collection<Class> =
        participantsAnalysis.getParticipants()

    fun getSuspensionsOnFuture(f: Future): Collection<SuspensionInfo> =
        actorActivityAnalysis.getSuspensionsOnFuture(f)

    fun getActiveFuture(actor: Class): Future? =
        actorActivityAnalysis.getActiveFuture(actor)

    fun getActiveFutures(): Collection<Future> =
        actorActivityAnalysis.getActiveFutures()

    fun getNonFreshFutures(): Set<Future> =
        futureFreshnessAnalysis.getNonFreshFutures()

    fun getFuturesToTargetMapping(): Map<Future, MethodBinding> =
        futureFreshnessAnalysis.getFuturesToTargetMapping()
}