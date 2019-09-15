package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.types.parser.FileContext
import org.abs_models.frontend.ast.PureExp

sealed class GlobalType {
    open val fileContext: FileContext? = null

    // 0 -f-> p:m
    data class Initialization(
        val f: Future,
        val c: Class,
        val m: Method,
        val postCondition: PureExp? = null,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "0 -${f.value}-> ${c.value}:${m.value}${
            if (postCondition != null) {
                "<$postCondition>"
            }
        
            else {
                ""
            }
        }"
    }

    data class Concatenation(
        val lhs: GlobalType,
        val rhs: GlobalType,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "$lhs.\n$rhs"
    }

    data class Interaction(
        val caller: Class,
        val f: Future,
        val callee: Class,
        val m: Method,
        val postCondition: PureExp? = null,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${caller.value} -${f.value}-> ${callee.value}:${m.value}${
            if (postCondition != null) {
                "<$postCondition>"
            }
        
            else {
                ""
            }
        }"
    }

    data class Resolution(
        val c: Class,
        val f: Future, // TODO add ADT constructor name
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${c.value} resolves ${f.value}"
    }

    data class Fetching( // TODO better name
        val c: Class,
        val f: Future, // TODO add ADT constructor name
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${c.value} fetches ${f.value}"
    }

    data class Release(
        val c: Class,
        val f: Future,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "Rel(${c.value}, ${f.value})"
    }

    data class Branching(
        val c: Class,
        val branches: List<GlobalType>,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${c.value}{${branches.map{ it.toString() }.intersperse(", ")}}"
    }

    data class Repetition(
        val repeatedType: GlobalType,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "($repeatedType)*"
    }

    object Skip: GlobalType() {
        override fun toString() = "skip"
    }

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
            is Skip -> visitor.visit(this)
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
    fun visit(type: GlobalType.Skip): ReturnT
}

// TODO move to utility library

fun List<String>.intersperse(divider: String) =
    if (isEmpty()) {
        ""
    }

    else {
        reduce{ acc, next -> "$acc$divider$next"}
    }
