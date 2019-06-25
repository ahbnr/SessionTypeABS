package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.Transferable

data class FutureFreshnessDomain(
    private val usedFutures: Set<Future> = emptySet()
): Mergeable<FutureFreshnessDomain>, Transferable<GlobalType, FutureFreshnessDomain>, Repeatable<FutureFreshnessDomain>
{
    override fun loopContained(beforeLoop: FutureFreshnessDomain, errorDescriptions: MutableList<String>): Boolean {
        beforeLoop.usedFutures.forEach {
            if (!usedFutures.contains(it)) {
                errorDescriptions.add("Future ${it.value} was used before the loop, but not anymore after the 1st iteration.")

                return false
            }
        }

        return true
    }

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

    fun isFresh(f: Future) = !usedFutures.contains(f)
}