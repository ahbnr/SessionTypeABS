package de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces

/**
 * Represents abstract state transitions on an abstract domain to process session types.
 *
 * All abstract domains should therefore implement this interface.
 */
interface Transferable<SessionTypeT, DomainT> {
   fun transfer(label: SessionTypeT): DomainT
}