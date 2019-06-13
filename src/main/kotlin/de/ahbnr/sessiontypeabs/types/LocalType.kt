package de.ahbnr.sessiontypeabs.types;

sealed class LocalType {
  class InvocationRecv(
    val f: Future,
    val m: Method
  ): LocalType()

  class Reactivation(
    val f: Future
  ): LocalType()

  class Concatenation(
    val lhs: LocalType,
    val rhs: LocalType
  ): LocalType()

  class Repetition(
    val repeatedType: LocalType
  ): LocalType()

  class Branching(
    val choices: List<LocalType>
  ): LocalType()
}
