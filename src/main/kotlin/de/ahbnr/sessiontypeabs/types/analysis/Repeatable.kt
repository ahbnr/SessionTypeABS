package de.ahbnr.sessiontypeabs.types.analysis

interface Repeatable<T> {
    fun loopContained(beforeLoop: T, errorDescriptions: MutableList<String>): Boolean
}