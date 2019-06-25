package de.ahbnr.sessiontypeabs.types.analysis

sealed class JoinSemiFlatLattice<T>
    : Joinable<JoinSemiFlatLattice<T>>, PartiallyOrdered<JoinSemiFlatLattice<T>> {
    data class Value<T>(
        val v: T
    ): JoinSemiFlatLattice<T>()

    class Any<T>(): JoinSemiFlatLattice<T>()

    override infix fun isLessOrEqualTo(rhs: JoinSemiFlatLattice<T>) =
        rhs == this || rhs is Any<T>

    override infix fun join(rhs: JoinSemiFlatLattice<T>) =
        if (rhs == this) {
            this
        }

        else {
            Any<T>()
        }
}