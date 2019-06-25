package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.types.parser.FileContext

sealed class GlobalType() {
    open val fileContext: FileContext? = null

    // 0 -f-> p:m
    data class Initialization(
        val f: Future,
        val c: Class,
        val m: Method,
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Concatenation(
        val lhs: GlobalType,
        val rhs: GlobalType,
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Interaction(
        val caller: Class,
        val f: Future,
        val callee: Class,
        val m: Method,
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Resolution(
        val c: Class,
        val f: Future, // TODO add ADT constructor name
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Fetching( // TODO better name
        val c: Class,
        val f: Future, // TODO add ADT constructor name
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Release(
        val c: Class,
        val f: Future,
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Branching(
        val c: Class,
        val branches: List<GlobalType>,
        override val fileContext: FileContext? = null
    ): GlobalType()

    data class Repetition(
        val repeatedType: GlobalType,
        override val fileContext: FileContext? = null
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

interface GlobalTypeVisitor<ReturnT> {
    fun visit(type: GlobalType.Repetition): ReturnT
    fun visit(type: GlobalType.Concatenation): ReturnT
    fun visit(type: GlobalType.Branching): ReturnT
    fun visit(type: GlobalType.Fetching): ReturnT
    fun visit(type: GlobalType.Resolution): ReturnT
    fun visit(type: GlobalType.Interaction): ReturnT
    fun visit(type: GlobalType.Initialization): ReturnT
    fun visit(type: GlobalType.Release): ReturnT
}
