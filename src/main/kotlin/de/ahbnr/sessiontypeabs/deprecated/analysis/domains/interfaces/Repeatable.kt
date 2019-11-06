package de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces

/**
 * In case a session type contains a repetition, the repeated type must be self-contained,
 * otherwise validating the protocol becomes a lot more difficult.
 *
 * All abstract analysis domains should therefore implement this interface, see description of
 * [selfContained]
 */
interface Repeatable<DomainT> {
    /**
     * Checks whether a loop session type is self contained.
     * It is called by [execute] after a loop type has been processed.
     *
     * Implementors should compare the state represented by [beforeLoop] with the current state
     * and return, whether the loop was self contained according to the logic of the domain.
     *
     * @param beforeLoop state before the loop has been processed
     * @param errorDescriptions implementors should store explanations for detected errors here, if false is returned
     * @return true iff the loop is self contained
     */
    fun selfContained(beforeLoop: DomainT, errorDescriptions: MutableList<String>): Boolean
}