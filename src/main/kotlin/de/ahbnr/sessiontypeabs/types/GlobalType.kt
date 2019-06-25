package de.ahbnr.sessiontypeabs.types

sealed class GlobalType {
    // 0 -f-> p:m
    data class Initialization(
        val f: Future,
        val c: Class,
        val m: Method
    ): GlobalType()

    data class Concatenation(
        val lhs: GlobalType,
        val rhs: GlobalType
    ): GlobalType()

    data class Interaction(
        val caller: Class,
        val f: Future,
        val callee: Class,
        val m: Method
    ): GlobalType()

    data class Resolution(
        val c: Class,
        val f: Future // TODO add ADT constructor name
    ): GlobalType()

    data class Fetching( // TODO better name
        val c: Class,
        val f: Future // TODO add ADT constructor name
    ): GlobalType()

    data class Release(
        val c: Class,
        val f: Future
    ): GlobalType()

    data class Branching(
        val c: Class,
        val branches: List<GlobalType>
    ): GlobalType()

    data class Repetition(
        val repeatedType: GlobalType
    ): GlobalType()

    fun <ReturnT> accept(visitor: GlobalTypeVisitor<ReturnT>): ReturnT =
        when (this) {
            is Initialization -> visitor.visit(this)
            is Concatenation -> visitor.visit(this)
            is Interaction -> visitor.visit(this)
            is Resolution -> visitor.visit(this)
            is Fetching -> visitor.visit(this)
            is Release -> visitor.visit(this)
            is Branching -> visitor.visit(this)
            is Repetition -> visitor.visit(this)
        }
}