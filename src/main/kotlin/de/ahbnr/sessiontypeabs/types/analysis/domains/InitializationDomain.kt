package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Transferable

/**
 * This domain ensures, that every global session type begins with an
 * initialization ([GlobalType.Initialization]).
 */
data class InitializationDomain(
    val initialized: Boolean = false
): Mergeable<InitializationDomain>,
    Transferable<GlobalType, InitializationDomain>,
    Repeatable<InitializationDomain>
{
    /**
     * A loop is always considered self-contained by this domain.
     * (An exception is only thrown in case of a programmer error)
     */
    override fun loopContained(beforeLoop: InitializationDomain, errorDescriptions: MutableList<String>): Boolean {
        if (this != beforeLoop) {
            throw RuntimeException("The initialization status before the loop and after the first iteration don't match, but should be equal. This should never happen and it is an error in this program.")
        }

        return true
    }

    private fun isInitialized() = initialized

    private fun enforceInitialization(label: GlobalType) =
        if (!isInitialized()) {
            throw TransferException(
                label,
                "The protocol must be initialized before any action other than initialization."
            )
        } else {
            this.copy()
        }

    private fun transfer(label: GlobalType.Initialization) =
        if (isInitialized()) {
            throw TransferException(
                label,
                "Protocol is initialized twice."
            )
        } else {
            this.copy(initialized = true)
        }

    override fun transfer(label: GlobalType) =
        when(label) {
            is GlobalType.Initialization -> transfer(label)
            else -> enforceInitialization(label)
        }

    override fun merge(rhs: InitializationDomain) =
        copy(initialized = initialized && rhs.initialized) // TODO: Correct choice?
}
