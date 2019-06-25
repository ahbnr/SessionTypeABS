package de.ahbnr.sessiontypeabs.types.analysis.domains

data class MapEqualityError<K, V>(
    val key: K,
    val lval: V,
    val rval: V
)

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
