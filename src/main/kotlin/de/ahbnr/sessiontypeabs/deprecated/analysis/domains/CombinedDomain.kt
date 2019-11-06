package de.ahbnr.sessiontypeabs.deprecated.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Finalizable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Transferable
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.ConfigurableAnalysis

/**
 * Combines all other domains into one analysis/validation mechanism for global session
 * types.
 */
@Deprecated("Use package [de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis] instead.")
data class CombinedDomain(
    private val classActivity: ClassActivityDomain = ClassActivityDomain(),
    private val classComputation: ClassComputationDomain = ClassComputationDomain(),
    private val futureFreshness: FutureFreshnessDomain = FutureFreshnessDomain(),
    private val protocolInitialization: InitializationDomain = InitializationDomain(),
    private val futureResolution: ResolutionDomain = ResolutionDomain(),
    private val participantsTracker: ParticipantsDomain = ParticipantsDomain()
): Mergeable<CombinedDomain>,
    Transferable<GlobalType, CombinedDomain>,
    Repeatable<CombinedDomain>,
    Finalizable<CombinedDomain>,
    ConfigurableAnalysis<CombinedDomain>
{
    // <workaround>
    override fun transfer(t: GlobalType) = this
    override fun merge(rhs: CombinedDomain, branchingContext: GlobalType.Branching) = this
    override fun selfContained(preState: CombinedDomain, context: GlobalType) {}
    override fun closeScopes(preState: CombinedDomain, context: GlobalType) = this
    // </workaround>

    /**
     * A loop is considered self-contained, iff it is being considered self-contained by all
     * composite domains.
     */
    override fun selfContained(beforeLoop: CombinedDomain, errorDescriptions: MutableList<String>) =
           classActivity.selfContained(beforeLoop.classActivity, errorDescriptions)
        && classComputation.selfContained(beforeLoop.classComputation, errorDescriptions)
        && futureFreshness.selfContained(beforeLoop.futureFreshness, errorDescriptions)
        && protocolInitialization.selfContained(beforeLoop.protocolInitialization, errorDescriptions)
        && futureResolution.selfContained(beforeLoop.futureResolution, errorDescriptions)
        && participantsTracker.selfContained(beforeLoop.participantsTracker, errorDescriptions)

    /**
     * The transferType relation is implemented by applying the transferType relation of all
     * composite domains.
     *
     * This in fact ensures, that the transferType relation can only be applied, iff the
     * transferType relation of all composite domains can be applied, since the
     * application of composite relation would throw an exception otherwise.
     */
    override fun transferType(label: GlobalType) =
        this.copy(
            classActivity = classActivity.transferType(label),
            classComputation = classComputation.transferType(label),
            futureFreshness = futureFreshness.transferType(label),
            protocolInitialization = protocolInitialization.transferType(label),
            futureResolution = futureResolution.transferType(label),
            participantsTracker = participantsTracker.transferType(label)
        )

    /**
     * The composition of domains is merged by merging all composite domains
     */
    override fun merge(rhs: CombinedDomain) =
        this.copy(
            classActivity = classActivity merge rhs.classActivity,
            classComputation = classComputation merge rhs.classComputation,
            futureFreshness = futureFreshness merge rhs.futureFreshness,
            protocolInitialization = protocolInitialization merge rhs.protocolInitialization,
            futureResolution = futureResolution merge rhs.futureResolution,
            participantsTracker = participantsTracker merge rhs.participantsTracker
        )

    override fun closeScope(finalizedType: GlobalType) = this.copy()

    fun getSuspensionsOnFuture(f: Future) =
        classActivity
            .getClassesSuspendedOnFuture(f).mapNotNull {
                c -> classComputation
                    .getCurrentFutureForClass(c)
                    ?.let { SuspensionInfo(c, it) }
            }

    fun getUsedFutures(): Set<Future> =
        futureFreshness.usedFutures

    fun getActiveFutures(): Set<Future> =
        getParticipants()
            .mapNotNull { getActiveFuture(it) }
            .toSet()

    fun getActiveFuture(c: Class): Future? =
        if (classActivity.isActive(c)) {
            classComputation.getCurrentFutureForClass(c)
        }

        else {
            null
        }

    fun getFuturesToTargetMapping() = futureFreshness.futuresToTargets

    fun getParticipants() =
        participantsTracker.participants

}

data class SuspensionInfo (
    val suspendedClass: Class,
    val futureAfterReactivation: Future
)
