package de.ahbnr.sessiontypeabs.deprecated.analysis.domains.utils

/**
 * Records a pair of non-equal values with the same key in the two maps passed to [areMapsWithDefaultValEqual]
 */
data class MapEqualityError<K, V>(
    val key: K,
    val lval: V,
    val rval: V
)

/**
 * Compares maps for equality by comparing the values with the same key in both maps for equality.
 * If a map does not contain a value for a key in the other map, [defaul] is used instead.
 *
 * It is used by many abstract domains to check session type repetitions for self-containedness,
 * see also [Repeatable]
 *
 * @return null iff the maps are equal, see [MapEqualityError] documentation otherwise
 */
fun <K, V> areMapsWithDefaultValEqual(default: V, lhs: Map<K, V>, rhs: Map<K, V>): MapEqualityError<K, V>? {
    val keys = lhs.keys union rhs.keys

    for (key in keys) {
        val lval = lhs.getOrDefault(key, default)
        val rval = rhs.getOrDefault(key, default)

        if (lval != rval) {
            return MapEqualityError(key, lval, rval)
        }
    }

    return null
}
