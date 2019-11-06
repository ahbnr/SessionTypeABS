package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.intersperse
import org.abs_models.frontend.ast.PureExp

sealed class CondensedType {
    data class InvocationRecv(
        val f: Future,
        val m: Method,
        val postCondition: PureExp? = null
    ): CondensedType() {
        override fun toString() = "0?${f.value}:${m.value}${
            if (postCondition != null) {
                "<$postCondition>"
            }

            else {
                ""
            }
        }"
    }

    data class Reactivation(
        val f: Future
    ): CondensedType() {
        override fun toString() = "React(${f.value})"
    }

    data class Concatenation(
        val lhs: CondensedType,
        val rhs: CondensedType
    ): CondensedType() {
        override fun toString() = "$lhs.\n$rhs"
    }

    data class Repetition(
        val repeatedType: CondensedType
    ): CondensedType() {
        override fun toString() = "($repeatedType)*"
    }

    data class Branching(
        val choices: List<CondensedType>
    ): CondensedType() {
        override fun toString() = "{${choices.map{ it.toString() }.intersperse(", ")}}"
    }

    object Skip: CondensedType() {
        override fun toString() = "skip"
    }

    fun <ResultT> accept(visitor: CondensedTypeVisitor<ResultT>): ResultT =
        when (this) {
            is Branching -> visitor.visit(this)
            is InvocationRecv -> visitor.visit(this)
            is Concatenation -> visitor.visit(this)
            is Repetition -> visitor.visit(this)
            is Skip -> visitor.visit(this)
            is Reactivation -> visitor.visit(this)
        }
}


interface CondensedTypeVisitor<ResultT> {
    fun visit(type: CondensedType.Reactivation): ResultT
    fun visit(type: CondensedType.InvocationRecv): ResultT
    fun visit(type: CondensedType.Concatenation): ResultT
    fun visit(type: CondensedType.Repetition): ResultT
    fun visit(type: CondensedType.Branching): ResultT
    fun visit(type: CondensedType.Skip): ResultT
}