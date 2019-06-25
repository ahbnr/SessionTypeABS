package de.ahbnr.sessiontypeabs.types.analysis

interface Mergeable<T> {
    infix fun merge(rhs: T): T
}
