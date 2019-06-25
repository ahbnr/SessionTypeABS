package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.*
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable

private typealias ResolutionState = JoinSemiFlatLattice<Boolean>
private typealias UnknownResolutionState = JoinSemiFlatLattice.Any<Boolean>
private typealias KnownResolutionState = JoinSemiFlatLattice.Value<Boolean>

data class ResolutionDomain(
    private val resolutionState: Map<Future, ResolutionState> = emptyMap()
): Mergeable<ResolutionDomain>, Transferable<GlobalType, ResolutionDomain>, Repeatable<ResolutionDomain>
{
    override fun loopContained(beforeLoop: ResolutionDomain, errorDescriptions: MutableList<String>): Boolean {
        val maybeError = areMapsWithDefaultValEqual(
            KnownResolutionState(true),
            resolutionState,
            beforeLoop.resolutionState
        )

        if (maybeError != null) {
            errorDescriptions.add(
                "Before the loop, future ${maybeError.key.value} had resolution state ${maybeError.rval}, afterwards ${maybeError.lval}."
            )

            return false
        }

        return true
    }

    private fun getResolutionState(f: Future) =
        resolutionState.getOrDefault(f, KnownResolutionState(true))

    private fun updateResolutionState(f: Future, state: ResolutionState) =
        this.copy(
            resolutionState = resolutionState.plus(
                f to state
            )
        )

    private fun transfer(label: GlobalType.Initialization): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    true -> return updateResolutionState(label.f,
                        KnownResolutionState(false)
                    )
                    false -> throw TransferException(
                        label,
                        "Can not initialize future ${label.f.value}, since it is currently being computed."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not initialize future ${label.f.value}, since it might currently be active."
                )
        }
    }

    private fun transfer(label: GlobalType.Interaction): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    true -> return updateResolutionState(label.f,
                        KnownResolutionState(false)
                    )
                    false -> throw TransferException(
                        label,
                        "Can not create future ${label.f.value} for interaction, since it is currently being computed."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not create future ${label.f.value}, since it might currently be active."
                )
        }
    }

    private fun transfer(label: GlobalType.Resolution): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    false -> return updateResolutionState(label.f,
                        KnownResolutionState(true)
                    )
                    true -> throw TransferException(
                        label,
                        "Can not resolve future ${label.f.value}, since it has already been resolved or is not active."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not resolve future ${label.f.value}, since it might already have been resolved."
                )
        }
    }

    private fun transfer(label: GlobalType.Fetching): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    true -> return this.copy()
                    false -> throw TransferException(
                        label,
                        "Can not fetch from future ${label.f}, since it has not been resolved yet."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not fetch from future ${label.f}, since it might not have been resolved yet."
                )
        }
    }

    private fun transfer(label: GlobalType.Release): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    false -> return this.copy()
                    true -> throw TransferException(
                        label,
                        "Can not release control until future ${label.f.value} completes, since it has already been resolved."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not release control until future ${label.f.value} completes, since it might already have been resolved."
                )
        }
    }

    override fun transfer(label: GlobalType) =
        when (label) {
            is GlobalType.Initialization -> transfer(label)
            is GlobalType.Interaction -> transfer(label)
            is GlobalType.Resolution -> transfer(label)
            is GlobalType.Fetching -> transfer(label)
            is GlobalType.Release -> transfer(label)
            else -> this.copy()
        }

    override fun merge(rhs: ResolutionDomain) =
        this.copy(resolutionState = resolutionState join resolutionState)
}