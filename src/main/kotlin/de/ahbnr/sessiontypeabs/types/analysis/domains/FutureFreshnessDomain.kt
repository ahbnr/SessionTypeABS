package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Transferable

/**
 * Future identifiers may only be initialized once by initialization or interaction types and
 * not be used before then.
 *
 * This domain keeps track of this.
 */
data class FutureFreshnessDomain(
    private val usedFutures: Set<Future> = emptySet()
): Mergeable<FutureFreshnessDomain>,
    Transferable<GlobalType, FutureFreshnessDomain>,
    Repeatable<FutureFreshnessDomain>
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
    private fun introduceFuture(newFuture: Future, label: GlobalType) =
        if (!isFresh(newFuture)) {
            throw TransferException(
                label,
                "Cannot introduce future ${newFuture.value}, since this future has already been introduced."
            )
        }

        else {
            this.copy(
                usedFutures = usedFutures.plus(newFuture)
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

    private fun transfer(label: GlobalType.Initialization) = introduceFuture(label.f, label)
    private fun transfer(label: GlobalType.Interaction) = introduceFuture(label.f, label)

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

    override fun merge(rhs: FutureFreshnessDomain) =
        this.copy(
            usedFutures = usedFutures union rhs.usedFutures
        )

    /**
     * A future is considered to be fresh, if its identifier has not yet been
     * created during application of the [transfer] relation on an interaction or initialization .
     */
    private fun isFresh(f: Future) = !usedFutures.contains(f)
}