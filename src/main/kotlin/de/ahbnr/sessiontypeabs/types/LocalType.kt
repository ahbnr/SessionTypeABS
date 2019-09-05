package de.ahbnr.sessiontypeabs.types

import org.abs_models.frontend.ast.PureExp

sealed class LocalType {
    data class Initialization(
        val f: Future,
        val m: Method,
        val postCondition: PureExp? = null
    ): LocalType()

    data class Sending(
        val receiver: Class,
        val f: Future,
        val m: Method
    ): LocalType()

    data class Receiving(
        val sender: Class,
        val f: Future,
        val m: Method,
        val postCondition: PureExp? = null
    ): LocalType()

    data class Resolution( // = Put
        val f: Future // TODO add ADT constructor name
    ): LocalType()

    data class Fetching( // = Get
        val f: Future // TODO add ADT constructor name
    ): LocalType()

    data class Suspension( // Await, TODO maybe call it Release, too, to redce confusion
        val suspendedFuture: Future,
        val awaitedFuture: Future
    ): LocalType()

    data class Reactivation(
        val f: Future
    ): LocalType()

    // ⊕{Lj}
    data class Choice(
        val choices: List<LocalType>
    ): LocalType()

    // &f{Lj}
    data class Offer(
        val f: Future,
        val branches: List<LocalType>
    ): LocalType()

    data class Concatenation(
        val lhs: LocalType,
        val rhs: LocalType
    ): LocalType()

    data class Repetition(
        val repeatedType: LocalType
    ): LocalType()

    object Skip: LocalType()

    object Termination: LocalType() // TODO integrate in projection execution etc. Relevant in branching.

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