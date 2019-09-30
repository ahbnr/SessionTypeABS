package de.ahbnr.sessiontypeabs.types

import org.abs_models.frontend.ast.PureExp

sealed class LocalType {
    data class Initialization(
        val f: Future,
        val m: Method,
        val postCondition: PureExp? = null
    ): LocalType() {
        override fun toString() = "0?${f.value}:${m.value}${
            if (postCondition != null) {
                "<$postCondition>"
            }

            else {
                ""
            }
        }"
    }

    data class Sending(
        val receiver: Class,
        val f: Future,
        val m: Method
    ): LocalType() {
        override fun toString() = "${receiver.value}!${f.value}:${m.value}"
    }

    data class Receiving(
        val sender: Class,
        val f: Future,
        val m: Method,
        val postCondition: PureExp? = null
    ): LocalType() {
        override fun toString() = "${sender.value}?${f.value}:${m.value}${
            if (postCondition != null) {
                "<$postCondition>"
            }

            else {
                ""
            }
        }"
    }

    data class Resolution( // = Put
        val f: Future,
        val constructor: ADTConstructor?
    ): LocalType() {
        override fun toString() = "Put ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"
    }

    data class Fetching( // = Get
        val f: Future,
        val constructor: ADTConstructor?
    ): LocalType() {
        override fun toString() = "Get ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"
    }

    data class Suspension( // Await, TODO maybe call it Release, too, to redce confusion
        val suspendedFuture: Future,
        val awaitedFuture: Future
    ): LocalType() {
        override fun toString() = "Await (${suspendedFuture.value}, ${awaitedFuture.value})"
    }

    data class Reactivation(
        val f: Future
    ): LocalType() {
        override fun toString() = "React(${f.value})"
    }

    // ⊕{Lj}
    data class Choice(
        val choices: List<LocalType>
    ): LocalType() {
        override fun toString() = "⊕{${choices.map{ it.toString() }.intersperse(", ")}}"
    }

    // &f{Lj}
    data class Offer(
        val f: Future,
        val branches: List<LocalType>
    ): LocalType() {
        override fun toString() = "&${f.value}{${branches.map{ it.toString() }.intersperse(", ")}}"
    }

    data class Concatenation(
        val lhs: LocalType,
        val rhs: LocalType
    ): LocalType() {
        override fun toString() = "$lhs.\n$rhs"
    }

    data class Repetition(
        val repeatedType: LocalType
    ): LocalType() {
        override fun toString() = "($repeatedType)*"
    }

    object Skip: LocalType() {
        override fun toString() = "skip"
    }

    object Termination: LocalType() {// TODO integrate in projection execution etc. Relevant in branching.
        override fun toString() = "end"
    }

    fun <ReturnT> accept(visitor: LocalTypeVisitor<ReturnT>): ReturnT =
        when (this) {
            is Initialization -> visitor.visit(this)
            is Sending -> visitor.visit(this)
            is Receiving -> visitor.visit(this)
            is Resolution -> visitor.visit(this)
            is Fetching -> visitor.visit(this)
            is Suspension -> visitor.visit(this)
            is Reactivation -> visitor.visit(this)
            is Choice -> visitor.visit(this)
            is Offer -> visitor.visit(this)
            is Concatenation -> visitor.visit(this)
            is Repetition -> visitor.visit(this)
            is Skip -> visitor.visit(this)
            is Termination -> visitor.visit(this)
        }
}

val LocalType.head: LocalType
    get() = when (this) {
        is LocalType.Concatenation -> this.lhs.head
        else -> this
    }

val LocalType.tail: LocalType?
    get() = when (this) {
        is LocalType.Concatenation ->
            if (this.lhs is LocalType.Concatenation) {
                LocalType.Concatenation(this.lhs.tail!!, this.rhs)
            }

            else {
                this.rhs
            }
        else -> null
    }

infix fun LocalType?.concat(rhs: LocalType?): LocalType? =
    when {
        this == null -> rhs
        rhs == null -> this
        else -> LocalType.Concatenation(
            lhs = this,
            rhs = this
        )
    }

interface LocalTypeVisitor<ReturnT> {
    fun visit(type: LocalType.Initialization): ReturnT

    fun visit(type: LocalType.Sending): ReturnT

    fun visit(type: LocalType.Receiving): ReturnT

    fun visit(type: LocalType.Resolution): ReturnT

    fun visit(type: LocalType.Fetching): ReturnT

    fun visit(type: LocalType.Suspension): ReturnT

    fun visit(type: LocalType.Reactivation): ReturnT

    fun visit(type: LocalType.Choice): ReturnT

    fun visit(type: LocalType.Offer): ReturnT

    fun visit(type: LocalType.Concatenation): ReturnT

    fun visit(type: LocalType.Repetition): ReturnT

    fun visit(type: LocalType.Skip): ReturnT

    fun visit(type: LocalType.Termination): ReturnT
}