package de.ahbnr.sessiontypeabs

fun <K, V> Collection<Map<K, V>>.mergeMaps(): Map<K, V> =
    fold(
        emptyMap(),
        {acc, nextMap ->
            acc + nextMap
        }
    )

fun <T> MutableCollection<T>.removeSomeElement(): T {
    require(this.isNotEmpty()) {"There is no element to remove in this collection."}

    val i = this.iterator()
    val e = i.next()
    i.remove()

    return e
}

// https://stackoverflow.com/questions/35808022/kotlin-list-tail-function
val <T> List<T>.tail: List<T>
    get() = drop(1)

val <T> List<T>.head: T
    get() = first()

fun <T> Set<T>.isSubsetOf(rhs: Collection<T>): Boolean =
    this.all { it in rhs }