package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.*
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.domains.utils.JoinSemiFlatLattice
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.FinalizationException
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.types.intersperse

private typealias ResolutionState = JoinSemiFlatLattice<Boolean>
private typealias UnknownResolutionState = JoinSemiFlatLattice.Any<Boolean>
private typealias KnownResolutionState = JoinSemiFlatLattice.Value<Boolean>

/**
 * Keeps track, whether a future has been resolved yet or not.
 */
data class ResolutionDomain(
    private val resolutionState: Map<Future, ResolutionState> = emptyMap(),
    private val futuresIntroducedInCurrentScope: Set<Future> = emptySet()
): Mergeable<ResolutionDomain>,
    Transferable<GlobalType, ResolutionDomain>,
    Repeatable<ResolutionDomain>,
    Finalizable<ResolutionDomain>
{
    /**
     * A repeated global type is considered self-contained, iff the resolution state of no future that was already
     * present before the loop changed during the iteration.
     */
    override fun loopContained(beforeLoop: ResolutionDomain, errorDescriptions: MutableList<String>): Boolean {
        // Search for a future, whose resolution state changed after the loop
        val maybeSelfContainednessViolation = (
            // don't consider new futures introduced in the loop, since them being resolved does
            // not affect self-containedness
            beforeLoop.resolutionState
                .asSequence()
                .find { (future, isResolved) ->
                    !resolutionState.containsKey(future)
                        ||
                    resolutionState.getValue(future) != isResolved
                }
        )

        if (maybeSelfContainednessViolation != null) {
            val maybePostIterationState = resolutionState[maybeSelfContainednessViolation.key]

            when (maybePostIterationState) {
                null -> throw RuntimeException("A tracked future has been lost during execution of a loop iteration. This should never happen")
                else ->
                    errorDescriptions.add(
                        "Before the loop, future ${maybeSelfContainednessViolation.key.value} had resolution state ${maybeSelfContainednessViolation.value}, afterwards $maybePostIterationState."
                    )
            }

            return false
        }

        return true
    }

    private fun getResolutionState(f: Future) =
        resolutionState.getOrDefault(f, KnownResolutionState(false))

    private fun updateResolutionState(f: Future, state: ResolutionState) =
        this.copy(
            resolutionState = resolutionState.plus(
                f to state
            )
        )

    /**
     * Check whether a future has already been resolved when it is created during an initialization.
     */
    private fun transfer(label: GlobalType.Initialization): ResolutionDomain {
        val resState = getResolutionState(label.f)

        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    false -> return this.copy(
                        futuresIntroducedInCurrentScope = futuresIntroducedInCurrentScope + label.f
                    )
                    true -> throw TransferException(
                        label,
                        "Can not initialize future ${label.f.value}, since it has already been resolved."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not initialize future ${label.f.value}, since it might already have been resolved."
                )
        }
    }

    /**
     * Check whether a future has already been resolved when it is created during an interaction.
     */
    private fun transfer(label: GlobalType.Interaction): ResolutionDomain {
        val resState = getResolutionState(label.f)

        return when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    false -> this.copy(
                        futuresIntroducedInCurrentScope = futuresIntroducedInCurrentScope + label.f
                    )
                    true -> throw TransferException(
                        label,
                        "Can not create future ${label.f.value} for interaction, since it has already been resolved."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not create future ${label.f.value}, since it might already have been resolved."
                )
        }
    }

    /**
     * Set a future to being resolved when encountering a resolution type, but
     * check first, that it hasn't been resolved before.
     */
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

    /**
     * A future can only be fetched, if it has been resolved before.
     */
    private fun transfer(label: GlobalType.Fetching): ResolutionDomain {
        val resState = getResolutionState(label.f)
        when(resState)
        {
            is KnownResolutionState ->
                when (resState.v) {
                    true -> return this.copy()
                    false -> throw TransferException(
                        label,
                        "Can not fetch from future ${label.f.value}, since it has not been resolved yet."
                    )
                }
            is UnknownResolutionState ->
                throw TransferException(
                    label,
                    "Can not fetch from future ${label.f.value}, since it might not have been resolved yet."
                )
        }
    }

    /**
     * Control can only be released until a future f is being resolved, if
     * it hasn't been resolved already.
     */
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

    override fun finalizeScope(finalizedType: GlobalType): ResolutionDomain {
        val unresolvedLocalFutures = futuresIntroducedInCurrentScope.filterNot(this::isGuaranteedToBeResolved)

        return if (unresolvedLocalFutures.isEmpty()) {
            this.copy(
                futuresIntroducedInCurrentScope = emptySet()
            )
        }

        else {
            throw FinalizationException(
                type = finalizedType,
                message = """
                    |Global session type does not specify a resolving action for every future in the current scope.
                    |Every type contained in a branching or repeating type must resolve all futures created in it, as
                    |must the protocol as a whole.
                    |
                    |Unresolved futures: ${unresolvedLocalFutures.map { it.value }.intersperse(", ")}
                """.trimMargin()
            )
        }
    }

    private fun isGuaranteedToBeResolved(f: Future) =
        when (val resState = getResolutionState(f)) {
            is KnownResolutionState -> resState.v
            is UnknownResolutionState -> false
        }
}