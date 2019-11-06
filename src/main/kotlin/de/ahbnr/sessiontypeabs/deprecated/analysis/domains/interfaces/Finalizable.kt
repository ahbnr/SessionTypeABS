package de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces

import de.ahbnr.sessiontypeabs.types.GlobalType

/**
 * Represents a finalizing action on an abstract domain upon finishing abstract execution of a session type or
 * of a type nested in a branching or repeating type.
 *
 * All abstract domains should therefore implement this interface.
 */
interface Finalizable<DomainT> {
   fun closeScope(finalizedType: GlobalType): DomainT
}