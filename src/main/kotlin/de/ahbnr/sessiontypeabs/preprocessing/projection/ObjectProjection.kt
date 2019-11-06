package de.ahbnr.sessiontypeabs.preprocessing.projection

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.GlobalTypeVisitor
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.AnalyzedGlobalTypeVisitor
import de.ahbnr.sessiontypeabs.types.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.preprocessing.projection.exceptions.ProjectionException

// TODO Check if post-conditions have to be modified on projection

fun project(type: AnalyzedGlobalType<CombinedAnalysis>) =
    type
        .postState
        .getParticipants()
        .map{ participant -> participant to project(
            type,
            participant
        )
        }
        .toMap()

fun project(type: AnalyzedGlobalType<CombinedAnalysis>, c: Class): AnalyzedLocalType =
    type.accept(ObjectProjector(c))


class ObjectProjector(
    val c: Class
): AnalyzedGlobalTypeVisitor<CombinedAnalysis, AnalyzedLocalType> {
    override fun visit(type: AnalyzedGlobalType.TerminalType<CombinedAnalysis>): AnalyzedLocalType {
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
                        f = type.f,
                        constructor = type.constructor
                    )
                    else -> LocalType.Skip
                }

            override fun visit(type: GlobalType.Resolution): LocalType =
                when (c) {
                    // is the class we project on resolving the future?
                    type.c -> LocalType.Resolution(
                        f = type.f,
                        constructor = type.constructor
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

            override fun visit(type: GlobalType.Skip) = LocalType.Skip
        }


        return AnalyzedLocalType.TerminalType(
            type = type.type.accept(visitor),
            preState = type.preState,
            postState = type.postState
        )
    }

    override fun visit(type: AnalyzedGlobalType.ConcatenationType<CombinedAnalysis>): AnalyzedLocalType {
        val leftProjection = type.leftAnalyzedType.accept(this)
        val rightProjection = type.rightAnalyzedType.accept(this)

        return when {
            leftProjection.type == LocalType.Skip -> rightProjection
            rightProjection.type == LocalType.Skip -> leftProjection
            else -> AnalyzedLocalType.ConcatenationType(
                type = LocalType.Concatenation(leftProjection.type, rightProjection.type),
                preState = type.preState,
                postState = type.postState,
                leftAnalyzedType = leftProjection,
                rightAnalzedType = rightProjection
            )
        }
    }

    override fun visit(type: AnalyzedGlobalType.BranchingType<CombinedAnalysis>): AnalyzedLocalType {
        val branches = type
            .analyzedBranches
            .map{ branch -> branch.accept(this) }

        val preState = type.preState
        val postState = type.postState

        // Either...
        when {
            // ...there are no branches, so we can simply skip this part
            branches.isEmpty() -> return AnalyzedLocalType.TerminalType(
                type = LocalType.Skip,
                preState = preState,
                postState = postState
            )

            // ...OR the object we are projecting on makes the choice, in which case we generate a choice type
            type.getCastedType().choosingActor == c -> // TODO: Check for activity of choosingActor, otherwise it isnt able to make a choice? Does the abstract execution do this?
                return AnalyzedLocalType.ChoiceType(
                    type = LocalType.Choice(
                        choices = branches.map { it.type }
                    ),
                    preState = preState,
                    postState = postState,
                    analyzedChoices = branches
                )

            // ...OR we are offered branches by another actor (though we are possibly not participating)
            else -> {
                val maybeChooserFuture = type.preState.getActiveFuture(type.getCastedType().choosingActor)

                if (maybeChooserFuture != null) {
                    return AnalyzedLocalType.OfferType(
                        type = LocalType.Offer(
                            branches = branches.map { it.type },
                            f = maybeChooserFuture
                        ),
                        preState = preState,
                        postState = postState,
                        analyzedBranches = branches
                    )
                }

                else {
                    throw ProjectionException(
                        type.type,
                        "The actor making the branching choice has no active future."
                    )
                }
            }
        }
    }

    override fun visit(type: AnalyzedGlobalType.RepetitionType<CombinedAnalysis>): AnalyzedLocalType {
        val projection = type.analyzedRepeatedType.accept(this)

        return when {
            projection.type == LocalType.Skip -> projection
            else ->
                AnalyzedLocalType.RepetitionType(
                    type = LocalType.Repetition(projection.type),
                    preState = type.preState,
                    postState = type.postState,
                    analyzedRepeatedType = projection
                )
        }
    }
}