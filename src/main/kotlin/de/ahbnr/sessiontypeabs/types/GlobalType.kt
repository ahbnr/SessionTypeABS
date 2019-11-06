package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.intersperse
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

        override val isAtomic: Boolean
            get() = true
    }

    data class Concatenation(
        val lhs: GlobalType,
        val rhs: GlobalType,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "$lhs.\n$rhs"

        override val isAtomic: Boolean
            get() = false
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

        override val isAtomic: Boolean
            get() = true
    }

    data class Resolution(
        val c: Class,
        val f: Future,
        val constructor: ADTConstructor?,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${c.value} resolves ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"

        override val isAtomic: Boolean
            get() = true
    }

    data class Fetching(
        val c: Class,
        val f: Future,
        val constructor: ADTConstructor?,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${c.value} fetches ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"

        override val isAtomic: Boolean
            get() = true
    }

    data class Release(
        val c: Class,
        val f: Future,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "Rel(${c.value}, ${f.value})"

        override val isAtomic: Boolean
            get() = true
    }

    data class Branching(
        val choosingActor: Class,
        val branches: List<GlobalType>,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "${choosingActor.value}{${branches.map{ it.toString() }.intersperse(", ")}}"

        override val isAtomic: Boolean
            get() = false
    }

    data class Repetition(
        val repeatedType: GlobalType,
        override val fileContext: FileContext? = null
    ): GlobalType() {
        override fun toString() = "($repeatedType)*"

        override val isAtomic: Boolean
            get() = false
    }

    object Skip: GlobalType() {
        override fun toString() = "skip"

        override val isAtomic: Boolean
            get() = true
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

    abstract val isAtomic: Boolean
}

val GlobalType.head: GlobalType
    get() = when (this) {
        is GlobalType.Concatenation -> this.lhs.head
        else -> this
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

