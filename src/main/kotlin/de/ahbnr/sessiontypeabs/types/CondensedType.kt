package de.ahbnr.sessiontypeabs.types

sealed class CondensedType {
    data class InvocationRecv(
        val f: Future,
        val m: Method
    ): CondensedType()

    data class Reactivation(
        val f: Future
    ): CondensedType()

    data class Concatenation(
        val lhs: CondensedType,
        val rhs: CondensedType
    ): CondensedType()

    data class Repetition(
        val repeatedType: CondensedType
    ): CondensedType()

    data class Branching(
        val choices: List<CondensedType>
    ): CondensedType()

    object Skip: CondensedType()

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