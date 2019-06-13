package de.ahbnr.sessiontypeabs.types;

sealed class LocalType {
  data class InvocationRecv(
    val f: Future,
    val m: Method
  ): LocalType()

  data class Reactivation(
    val f: Future
  ): LocalType()

  data class Concatenation(
    val lhs: LocalType,
    val rhs: LocalType
  ): LocalType()

  data class Repetition(
    val repeatedType: LocalType
  ): LocalType()

  data class Branching(
    val choices: List<LocalType>
  ): LocalType()
}
