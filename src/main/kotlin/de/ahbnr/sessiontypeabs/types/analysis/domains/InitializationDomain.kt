package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.Transferable

data class InitializationDomain(
    val initialized: Boolean = false
): Mergeable<InitializationDomain>, Transferable<GlobalType, InitializationDomain>, Repeatable<InitializationDomain>
{
    override fun loopContained(beforeLoop: InitializationDomain, errorDescriptions: MutableList<String>): Boolean {
        if (this != beforeLoop) {
            errorDescriptions.add("Initialization steps before the loop and after the first iteration don't match, but should be equal.")

            return false
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
            throw TransferException(label, "Protocol is initialized twice.")
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
