package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Method
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Finalizable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Transferable
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.MergeException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ProjectionException

/**
 * Future identifiers may only be initialized once by initialization or interaction types and
 * not be used before then.
 *
 * This domain keeps track of this.
 */
data class FutureFreshnessDomain(
    val usedFutures: Set<Future> = emptySet(),
    val futuresToTargets: Map<Future, Pair<Class, Method>> = emptyMap() // TODO: Might want to extract this into own domain
): Mergeable<FutureFreshnessDomain>,
    Transferable<GlobalType, FutureFreshnessDomain>,
    Repeatable<FutureFreshnessDomain>,
    Finalizable<FutureFreshnessDomain>
{
    /**
     * A loop is always considered self-contained by this domain.
     * (An exception is only thrown in case of a programmer error)
     */
    override fun loopContained(beforeLoop: FutureFreshnessDomain, errorDescriptions: MutableList<String>): Boolean {
        beforeLoop.usedFutures.forEach {
            if (!usedFutures.contains(it)) {
                throw RuntimeException("Future ${it.value} was used before the loop, but not anymore after the 1st iteration. This should never happen and is a programming error.")
            }
        }

        return true
    }

    /**
     * Marks a future identifier as created.
     * Is invoked when applying the [transfer] relation on initialization and
     * interaction types.
     */
    private fun introduceFuture(newFuture: Future, targetActor: Class, calledMethod: Method, label: GlobalType) =
        if (!isFresh(newFuture)) {
            throw TransferException(
                label,
                "Cannot introduce future ${newFuture.value}, since this future has already been introduced."
            )
        }

        else {
            this.copy(
                usedFutures = usedFutures + newFuture,
                futuresToTargets =  futuresToTargets + (newFuture to Pair(targetActor, calledMethod))
            )
        }

    /**
     * Ensures a future identifier has been created using [introduceFuture] before
     * it is used (in a release etc.).
     */
    private fun useFuture(f: Future, label: GlobalType) =
        if (isFresh(f)) {
            throw TransferException(
                label,
                "Can not use future ${f.value} here, since it has not yet been introduced by an interaction or initialization."
            )
        }

        else {
            this.copy()
        }

    private fun transfer(label: GlobalType.Initialization) = introduceFuture(label.f, label.c, label.m, label)
    private fun transfer(label: GlobalType.Interaction) = introduceFuture(label.f, label.callee, label.m, label)

    private fun transfer(label: GlobalType.Release) = useFuture(label.f, label)
    private fun transfer(label: GlobalType.Resolution) = useFuture(label.f, label)
    private fun transfer(label: GlobalType.Fetching) = useFuture(label.f, label)

    override fun transfer(label: GlobalType) =
        when (label) {
            is GlobalType.Initialization -> transfer(label)
            is GlobalType.Interaction ->    transfer(label)
            is GlobalType.Release ->        transfer(label)
            is GlobalType.Resolution ->     transfer(label)
            is GlobalType.Fetching ->       transfer(label)
            else -> this.copy()
        }

    override fun merge(rhs: FutureFreshnessDomain): FutureFreshnessDomain {
        val usedFuturesIntersection = usedFutures intersect rhs.usedFutures

        val futureReferencingDifferentTargets = usedFuturesIntersection.find {
            futuresToTargets[it] != rhs.futuresToTargets[it]
        }

        if (futureReferencingDifferentTargets != null) {
            throw MergeException(
                """|Duplicate future names are allowed in different branches, however, they must target the same actor and method.
                   |
                   |Future $futureReferencingDifferentTargets violates this requirement.
                   |It targets ${futuresToTargets[futureReferencingDifferentTargets]} and ${rhs.futuresToTargets[futureReferencingDifferentTargets]} in different branches.
                   |""".trimMargin()
            )
        }

        return this.copy(
            usedFutures = usedFutures union rhs.usedFutures,
            futuresToTargets = futuresToTargets + rhs.futuresToTargets
        )
    }

    /**
     * A future is considered to be fresh, if its identifier has not yet been
     * created during application of the [transfer] relation on an interaction or initialization .
     */
    private fun isFresh(f: Future) = !usedFutures.contains(f)

    override fun finalizeScope(finalizedType: GlobalType) = this.copy()
}