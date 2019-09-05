package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.GlobalTypeVisitor
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ProjectionException

// TODO Check if post-conditions have to be modified on projection

fun project(type: AnalyzedGlobalType<CombinedDomain>) =
    type
        .postState
        .getParticipants()
        .map{ participant -> participant to project(type, participant) }
        .toMap()

fun project(type: AnalyzedGlobalType<CombinedDomain>, c: Class) =
    type.accept(Projector(c))

class Projector(
    val c: Class
): AnalyzedGlobalTypeVisitor<CombinedDomain, LocalType> {
    override fun visit(type: AnalyzedGlobalType.TerminalType<CombinedDomain>): LocalType {
        val outerType = type
        val visitor = object: GlobalTypeVisitor<LocalType> {
            override fun visit(type: GlobalType.Repetition) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: GlobalType.Concatenation) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: GlobalType.Branching) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: GlobalType.Fetching) =
                when (c) {
                    type.c -> LocalType.Fetching(
                        f = type.f
                    )
                    else -> LocalType.Skip
                }

            override fun visit(type: GlobalType.Resolution): LocalType =
                when (c) {
                    // is the class we project on resolving the future?
                    type.c -> LocalType.Resolution(
                        f = type.f
                    )
                    else -> outerType // Otherwise check, if c is awaiting type.f
                        .preState
                        .getSuspensionsOnFuture(type.f)
                        .find { suspensionInfo -> suspensionInfo.suspendedClass == c }
                        // if so, reactivate c
                        ?.let{ suspensionInfo ->  LocalType.Reactivation(f = suspensionInfo.futureAfterReactivation) }
                        // otherwise skip this one
                        ?: LocalType.Skip
                }

            override fun visit(type: GlobalType.Interaction) =
                when (c) {
                    type.callee -> LocalType.Receiving(
                        sender = type.caller,
                        f = type.f,
                        m = type.m,
                        postCondition = type.postCondition
                    )
                    type.caller -> LocalType.Sending(
                        receiver = type.callee,
                        f = type.f,
                        m = type.m
                    )
                    else -> LocalType.Skip
                }

            override fun visit(type: GlobalType.Initialization) =
                when (c) {
                    type.c -> LocalType.Initialization(
                        f = type.f,
                        m = type.m,
                        postCondition = type.postCondition
                    )
                    else -> LocalType.Skip
                }

            override fun visit(type: GlobalType.Release) =
                when (c) {
                    type.c ->
                        LocalType.Suspension(
                            suspendedFuture = outerType
                                .preState
                                .getActiveFuture(c)!!, // analysis should have made sure, this future exists
                            awaitedFuture = type.f
                        )
                    else -> LocalType.Skip
                }
        }

        return type.type.accept(visitor)
    }

    override fun visit(type: AnalyzedGlobalType.ConcatenationType<CombinedDomain>): LocalType {
        val leftProjection = type.leftAnalyzedType.accept(this)
        val rightProjection = type.rightAnalzedType.accept(this)

        return when {
            leftProjection == LocalType.Skip -> rightProjection
            rightProjection == LocalType.Skip -> leftProjection
            else -> LocalType.Concatenation(leftProjection, rightProjection)
        }
    }

    override fun visit(type: AnalyzedGlobalType.BranchingType<CombinedDomain>): LocalType {
        val branches = type
            .analyzedBranches
            .map{ branch -> branch.accept(this) }

        if (branches.isEmpty()) {
            return LocalType.Skip
        }

        else if (type.getCastedType().c == c) { // TODO: Check for activity of c, otherwise it isnt able to make a choice?
            return LocalType.Choice(
                choices = branches
            )
        }

        else {
            val maybeActiveFuture = type.preState.getActiveFuture(c)

            when {
                maybeActiveFuture != null -> return LocalType.Offer(
                    branches = branches,
                    f = maybeActiveFuture
                )
                branches.all { it == branches.first() } // TODO: Relax equality constraint
                    -> return branches.first()
                else -> throw ProjectionException(
                    type.type,
                    "Global branching type cannot be projected on class ${c.value}, since neither is ${c.value} actively choosing a branch, nor has it an active future at all, nor do all branches resolve to the same local type for the class, such that there is no choice."
                ) // TODO: Use more fitting exception class
            }
        }
    }

    override fun visit(type: AnalyzedGlobalType.RepetitionType<CombinedDomain>) =
        LocalType.Repetition(type.analyzedRepeatedType.accept(this))
}