package de.ahbnr.sessiontypeabs

fun <K, V> Collection<Map<K, V>>.mergeMaps(): Map<K, V> =
    fold(
        emptyMap(),
        {acc, nextMap ->
            acc + nextMap
        }
    )

// https://stackoverflow.com/questions/35808022/kotlin-list-tail-function
val <T> List<T>.tail: List<T>
    get() = drop(1)

val <T> List<T>.head: T
    get() = first()