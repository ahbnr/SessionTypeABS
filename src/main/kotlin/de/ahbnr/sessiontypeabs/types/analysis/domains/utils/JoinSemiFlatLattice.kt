package de.ahbnr.sessiontypeabs.types.analysis.domains.utils

import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.Joinable
import de.ahbnr.sessiontypeabs.types.analysis.domains.interfaces.PartiallyOrdered

/**
 * See https://en.wikipedia.org/wiki/Semilattice.
 * A lattice is considered flat, if there are no order relations except with the top element T and reflexive ones:
 * ∀x. (x ≤ ⊤ ∧ x ≤ x ∧ ∀y. (x ≠ y ∧ y ≠ ⊤ → ¬(x ≤ y)))
 * (Attention: Type parameter T ≠ ⊤ (top))
 *
 * This ADT constructs a join semi flat lattice from a given type, it looks like this:
 *
 * ----⊤-----
 * |  |  |  |
 * a  b  c ...
 *
 * where a, b, c, ... ∈ T
 */
sealed class JoinSemiFlatLattice<T>
    : Joinable<JoinSemiFlatLattice<T>>,
    PartiallyOrdered<JoinSemiFlatLattice<T>> {

    data class Value<T>(
        val v: T
    ): JoinSemiFlatLattice<T>()

    // Top element (⊤) representation
    class Any<T>: JoinSemiFlatLattice<T>()

    override infix fun isLessOrEqualTo(rhs: JoinSemiFlatLattice<T>) =
        rhs == this || rhs is Any<T>

    override infix fun join(rhs: JoinSemiFlatLattice<T>) =
        if (rhs == this) {
            this
        }

        else {
            Any()
        }

    fun <ReturnT> accept(visitor: JoinSemiFlatLatticeVisitor<T, ReturnT>): ReturnT =
        when (this) {
            is Value<T> -> visitor.visit(this)
            is Any<T> -> visitor.visit(this)
        }
}

interface JoinSemiFlatLatticeVisitor<T, ReturnT> {
    fun visit(value: JoinSemiFlatLattice.Value<T>): ReturnT
    fun visit(any: JoinSemiFlatLattice.Any<T>): ReturnT
}