package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.LocalTypeVisitor

// L ↦ L̂, see https://doi.org/10.1007/978-3-319-47846-3_19
fun condenseType(type: LocalType): CondensedType {
    val visitor = object: LocalTypeVisitor<CondensedType> {
        override fun visit(type: LocalType.Repetition) =
            CondensedType.Repetition(
                repeatedType = type.repeatedType.accept(this)
            )

        override fun visit(type: LocalType.Concatenation): CondensedType {
            val maybeLeftCondensedType = type.lhs.accept(this)
            val maybeRightCondensedType = type.rhs.accept(this)

            return when {
                maybeLeftCondensedType == CondensedType.Skip -> maybeRightCondensedType
                maybeRightCondensedType == CondensedType.Skip -> maybeLeftCondensedType
                else -> CondensedType.Concatenation(
                    maybeLeftCondensedType,
                    maybeRightCondensedType
                )
            }
        }

        private fun visitBranchType(branches: List<LocalType>) =
            CondensedType.Branching(
                choices = branches
                    .map { choice -> choice.accept(this) }
            )

        override fun visit(type: LocalType.Choice) =
            visitBranchType(type.choices)

        override fun visit(type: LocalType.Offer) =
            visitBranchType(type.branches)

        override fun visit(type: LocalType.Fetching) = CondensedType.Skip

        override fun visit(type: LocalType.Sending) = CondensedType.Skip

        override fun visit(type: LocalType.Receiving) =
            CondensedType.InvocationRecv(
                f = type.f,
                m = type.m
            )

        override fun visit(type: LocalType.Initialization) =
            CondensedType.InvocationRecv(
                f = type.f,
                m = type.m
            )

        override fun visit(type: LocalType.Suspension) = CondensedType.Skip

        override fun visit(type: LocalType.Reactivation) =
            CondensedType.Reactivation(
                f = type.f
            )

        override fun visit(type: LocalType.Resolution) = CondensedType.Skip

        override fun visit(type: LocalType.Skip) = CondensedType.Skip

        // TODO deal with termination correctly
        override fun visit(type: LocalType.Termination) = CondensedType.Skip
    }

    return type.accept(visitor)
}