package de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces

/**
 * To merge abstract domains (see [Mergeable]) oftentimes the least upper bound
 * needs to be computed for their elements.
 *
 * This interface describes types for which a least upper bound can be computed.
 * Implementors should also implement [PartiallyOrdered] such that
 * `a isLessOrEqual (a join b)` and `b isLessOrEqual (a join b)`.
 */
interface Joinable<T> {
    /**
     * Computes the least upper bound for two values.
     */
    infix fun join(rhs: T): T
}

interface PartiallyOrdered<T> {
    infix fun isLessOrEqualTo(rhs: T): Boolean
}

/**
 * Many domains use maps to store their elements. This Joinable implementation
 * applies join to every element for the same key in both maps.
 *
 * Attention: If a key is unique to a map the key-value pair is simply included
 * in the resulting map, so make sure this matches with the least-upper-bound
 * definition of your domain.
 */
infix fun <K,V : Joinable<V>> Map<K,V>.join(rhs: Map<K,V>) =
    (this.asSequence() + rhs.asSequence())
        .distinct()
        .groupBy({ it.key }, { it.value })
        .mapValues {
            (_, values) ->
                values.reduce {
                    lhs, rhs -> lhs join rhs
                }
        }
