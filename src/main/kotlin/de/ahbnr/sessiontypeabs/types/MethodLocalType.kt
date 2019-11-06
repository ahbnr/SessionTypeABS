package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.intersperse

sealed class MethodLocalType {
    data class Sending(
        val receiver: Class,
        val f: Future,
        val m: Method
    ): MethodLocalType() {
        override fun toString() = "${receiver.value}!${f.value}:${m.value}"
    }

    data class Resolution( // = Put
        val f: Future,
        val constructor: ADTConstructor?
    ): MethodLocalType() {
        override fun toString() = "Put ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"
    }

    data class Fetching( // = Get
        val f: Future,
        val constructor: ADTConstructor?
    ): MethodLocalType() {
        override fun toString() = "Get ${f.value}${constructor?.let {"(${it.value})"} ?: ""}"
    }

    data class Suspension(
        val suspendedFuture: Future,
        val awaitedFuture: Future
    ): MethodLocalType() {
        override fun toString() = "Await (${suspendedFuture.value}, ${awaitedFuture.value})"
    }

    // ⊕{Lj}
    data class Choice(
        val choices: List<MethodLocalType>
    ): MethodLocalType() {
        override fun toString() = "⊕{${choices.map{ it.toString() }.intersperse(", ")}}"
    }

    // &f{Cj: Lj}
    data class Offer(
        val f: Future,
        val branches: Map<ADTConstructor, MethodLocalType>
    ): MethodLocalType() {
        override fun toString() = "&${f.value}{${branches.map{ "${it.key}: ${it.value}" }.intersperse(", ")}}"
    }

    data class Concatenation(
        val lhs: MethodLocalType,
        val rhs: MethodLocalType
    ): MethodLocalType() {
        override fun toString() = "$lhs.\n$rhs"
    }

    data class Repetition(
        val repeatedType: MethodLocalType
    ): MethodLocalType() {
        override fun toString() = "($repeatedType)*"
    }

    object Skip: MethodLocalType() {
        override fun toString() = "skip"
    }

    object Termination: MethodLocalType() {
        override fun toString() = "end"
    }

    fun <ReturnT> accept(visitor: MethodLocalTypeVisitor<ReturnT>): ReturnT =
        when (this) {
            is Sending -> visitor.visit(this)
            is Resolution -> visitor.visit(this)
            is Fetching -> visitor.visit(this)
            is Suspension -> visitor.visit(this)
            is Choice -> visitor.visit(this)
            is Offer -> visitor.visit(this)
            is Concatenation -> visitor.visit(this)
            is Repetition -> visitor.visit(this)
            is Skip -> visitor.visit(this)
            is Termination -> visitor.visit(this)
        }

    val head: MethodLocalType
        get() = when (this) {
            is Concatenation -> this.lhs.head
            else -> this
        }

    val tail: MethodLocalType?
        get() = when (this) {
            is Concatenation ->
                if (this.lhs is Concatenation) {
                    Concatenation(this.lhs.tail!!, this.rhs)
                }

                else {
                    this.rhs
                }
            else -> null
        }
}

interface MethodLocalTypeVisitor<ReturnT> {
    fun visit(type: MethodLocalType.Sending): ReturnT

    fun visit(type: MethodLocalType.Resolution): ReturnT

    fun visit(type: MethodLocalType.Fetching): ReturnT

    fun visit(type: MethodLocalType.Suspension): ReturnT

    fun visit(type: MethodLocalType.Choice): ReturnT

    fun visit(type: MethodLocalType.Offer): ReturnT

    fun visit(type: MethodLocalType.Concatenation): ReturnT

    fun visit(type: MethodLocalType.Repetition): ReturnT

    fun visit(type: MethodLocalType.Skip): ReturnT

    fun visit(type: MethodLocalType.Termination): ReturnT
}


infix fun MethodLocalType?.concat(rhs: MethodLocalType?): MethodLocalType? =
    when {
        this == null -> rhs
        rhs == null -> this
        else -> MethodLocalType.Concatenation(
            lhs = this,
            rhs = rhs
        )
    }

// TODO This function is used to determine the minimal prefix of offer branches. Is a flat count really the right way to do this?
val MethodLocalType?.flatSize: Int
    get() = this?.let {
        when (it) {
            is MethodLocalType.Sending,
                is MethodLocalType.Resolution,
                is MethodLocalType.Fetching,
                is MethodLocalType.Suspension,
                is MethodLocalType.Skip,
                is MethodLocalType.Termination,
                is MethodLocalType.Repetition,
                is MethodLocalType.Choice,
                is MethodLocalType.Offer -> 1
            is MethodLocalType.Concatenation ->
                it.lhs.flatSize + it.rhs.flatSize
        }
    } ?: 0
