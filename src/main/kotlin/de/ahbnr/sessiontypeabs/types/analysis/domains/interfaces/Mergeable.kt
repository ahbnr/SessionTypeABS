package de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces

/**
 * In case of a branching session type the analysis must know the least abstract analysis
 * state that covers all branches.
 *
 * All abstract domains should therefore implement this interface. For more information see
 * [merge].
 */
interface Mergeable<DomainT> {
    /**
     * Applied to abstract domains after branching to merge them into one domain which covers the
     * result state of all branches.
     *
     * Implementors should make the result as concrete as possible, usually by computing the
     * least upper bound of the domain elements.
     */
    infix fun merge(rhs: DomainT): DomainT
}
