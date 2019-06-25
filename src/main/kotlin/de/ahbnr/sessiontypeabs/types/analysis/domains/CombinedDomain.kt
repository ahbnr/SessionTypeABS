package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Transferable

/**
 * Combines all other domains into one analysis/validation mechanism for global session
 * types.
 */
data class CombinedDomain(
    private val classActivity: ClassActivityDomain = ClassActivityDomain(),
    private val classComputation: ClassComputationDomain = ClassComputationDomain(),
    private val futureFreshness: FutureFreshnessDomain = FutureFreshnessDomain(),
    private val protocolInitialization: InitializationDomain = InitializationDomain(),
    private val futureResolution: ResolutionDomain = ResolutionDomain(),
    private val participantsTracker: ParticipantsDomain = ParticipantsDomain()
): Mergeable<CombinedDomain>,
    Transferable<GlobalType, CombinedDomain>,
    Repeatable<CombinedDomain> {
    /**
     * A loop is considered self-contained, iff it is being considered self-contained by all
     * composite domains.
     */
    override fun loopContained(beforeLoop: CombinedDomain, errorDescriptions: MutableList<String>) =
           classActivity.loopContained(beforeLoop.classActivity, errorDescriptions)
        && classComputation.loopContained(beforeLoop.classComputation, errorDescriptions)
        && futureFreshness.loopContained(beforeLoop.futureFreshness, errorDescriptions)
        && protocolInitialization.loopContained(beforeLoop.protocolInitialization, errorDescriptions)
        && futureResolution.loopContained(beforeLoop.futureResolution, errorDescriptions)
        && participantsTracker.loopContained(beforeLoop.participantsTracker, errorDescriptions)

    /**
     * The transfer relation is implemented by applying the transfer relation of all
     * composite domains.
     *
     * This in fact ensures, that the transfer relation can only be applied, iff the
     * transfer relation of all composite domains can be applied, since the
     * application of composite relation would throw an exception otherwise.
     */
    override fun transfer(label: GlobalType) =
        this.copy(
            classActivity = classActivity.transfer(label),
            classComputation = classComputation.transfer(label),
            futureFreshness = futureFreshness.transfer(label),
            protocolInitialization = protocolInitialization.transfer(label),
            futureResolution = futureResolution.transfer(label),
            participantsTracker = participantsTracker.transfer(label)
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

    fun getSuspensionsOnFuture(f: Future) =
        classActivity
            .getClassesSuspendedOnFuture(f).mapNotNull {
                c -> classComputation
                    .getCurrentFutureForClass(c)
                    ?.let { SuspensionInfo(c, it) }
            }

    fun getActiveFuture(c: Class): Future? =
        if (classActivity.isActive(c)) {
            classComputation.getCurrentFutureForClass(c)
        }

        else {
            null
        }

    fun getParticipants() =
        participantsTracker.participants
}

data class SuspensionInfo (
    val suspendedClass: Class,
    val futureAfterReactivation: Future
)