package de.ahbnr.sessiontypeabs.types.analysis

import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.tail
import de.ahbnr.sessiontypeabs.types.*
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.MethodProjectionException

fun project(type: AnalyzedLocalType, targetActor: Class, targetFuture: Future) =
    type.accept(MethodProjector(targetActor, targetFuture))

class MethodProjector(
    val targetActor: Class,
    val targetFuture: Future
): AnalyzedLocalTypeVisitor<MethodLocalType> {
    private data class Split(
        val prefix: MethodLocalType?,
        val constructor: ADTConstructor,
        val postfix: MethodLocalType?
    )

    private fun splitOnGet(projectionTargetActor: Class, projectionTargetFuture: Future, resolvedFuture: Future, branch: MethodLocalType): Set<Split> =
        if (
            branch is MethodLocalType.Fetching &&
            branch.f == resolvedFuture &&
            branch.constructor != null
        ) {
            setOf(
                Split(
                    prefix = null,
                    constructor = branch.constructor,
                    postfix = null
                )
            )
        }

        else if (branch is MethodLocalType.Concatenation) {
            splitOnGet(projectionTargetActor, projectionTargetFuture, resolvedFuture, branch.lhs)
                .map { split -> split.copy(postfix =
                    split.postfix?.let {postfix ->
                        MethodLocalType.Concatenation(
                            lhs = postfix,
                            rhs = branch.rhs
                        )
                    } ?: branch.rhs
                ) } union
            splitOnGet(projectionTargetActor, projectionTargetFuture, resolvedFuture, branch.rhs)
                .map { split -> split.copy(prefix =
                split.prefix?.let {prefix ->
                    MethodLocalType.Concatenation(
                        lhs = branch.lhs,
                        rhs = prefix
                    )
                } ?: branch.lhs
            ) }
        }

        else {
            emptySet()
        }

    private data class BranchLabelSplit(
        val prefix: MethodLocalType?,
        val branches: Map<ADTConstructor, MethodLocalType>
    )

    private fun extractBranchLabels(projectionTargetActor: Class, projectionTargetFuture: Future, resolvedFuture: Future, branches: AnalyzedLocalType.OfferType): BranchLabelSplit? {
        if (branches.analyzedBranches.isEmpty()) {
            throw IllegalArgumentException(
                "This function should only be called on non-empty offer types. Thus, this is a programming error."
            )
        }

        // Compute all possible splits of all branches at Get types, where the future is the resolved future
        // from the parameters.
        // Also give every branch an index to identify it
        val splitBranches = branches
            .analyzedBranches
            .mapIndexed { idx, branch -> idx to splitOnGet(
                projectionTargetActor,
                projectionTargetFuture,
                resolvedFuture,
                project(branch, projectionTargetActor, projectionTargetFuture)
            ) }
            // Only collect branches, which contain the Get type in question
            .filter { it.second.isNotEmpty() }

        // If the above computation results in fewer results than the number of branches we
        // started with, then there are branches not containing the searched Get expression.
        // Thus...
        //   ...either the choice is not communicated by the return value of the choosing future
        //   ...or there is an error in the specification.
        // Therefore, we abort at this point
        if (splitBranches.size != branches.analyzedBranches.size) {
            return null
        }

        // Reminder: We now have a list with possible splits at Get-Types for each branch.
        // We want to consider only those splits, where the prefix is future equivalent for
        // every branch.
        // Therefore we can select any branch and check which of its splits yields prefixes, which
        // are shared among all branches.
        // To minimize the computational effort, we simply select the one branch with the fewest
        // split candidates:
        val prefixCandidates = splitBranches
            .minBy { it.second.size }

        if (prefixCandidates == null || prefixCandidates.second.isEmpty()) {
            throw RuntimeException(
                "When trying to find a starting point with the fewest split candidates, there were no starting points at all, or the minimal starting point has no candidates. This should never happen, since this case has already been checked. Thus, this is a programmer error."
            )
        }

        // We will collect viable results in this variable.
        val splits = mutableListOf<BranchLabelSplit>()

        // For every prefix in out set of candidates...
        for (prefixCandidate in prefixCandidates.second) {
            // Lets select a split with a future equivalent prefix for every branch
            val matchingPrefixSplits = splitBranches
                .mapNotNull { (idx, prefixSplits) ->
                    // if we are looking at the branch we got out candidate from, we can simply select
                    // the candidate itself and need no further computations
                    if (idx == prefixCandidates.first) {
                        prefixCandidate
                    }

                    // Otherwise select a future equivalent split, which also has a different constructor label
                    else {
                        prefixSplits
                            .find {
                                prefixCandidate.constructor != it.constructor &&
                                futureEquivalent(prefixCandidate.prefix, it.prefix)
                            }
                    }
                }

            // We found a viable prefix, if there is a future equivalent prefix with a different
            // constructor label for every branch. This in turn is the case, if the above computation
            // has a result for every branch, which means the collections have the same size
            if (matchingPrefixSplits.size == branches.analyzedBranches.size) {
                splits.add(
                    BranchLabelSplit(
                        prefixCandidate.prefix,
                        matchingPrefixSplits
                            .map { it.constructor to (it.postfix ?: MethodLocalType.Skip) }
                            .toMap()
                    )
                )
            }
        }

        // return the split with the shortest prefix.
        return splits
            .minBy { it.prefix.flatSize }
    }

    // FIXME: Loosen the future equivalence used here
    private fun identicalBranches(projectionTargetActor: Class, projectionTargetFuture: Future, branches: Collection<AnalyzedLocalType>): MethodLocalType? =
        if (branches.isEmpty()) {
            MethodLocalType.Skip
        }

        else {
            val projections = branches
                .map { project(it, projectionTargetActor, projectionTargetFuture) }

            val fixed = projections.head

            if (projections.tail.all {
                    futureEquivalent(fixed, it)
                }) {
                fixed
            }

            else {
                null
            }
        }


    override fun visit(analyzedLocalType: AnalyzedLocalType.TerminalType): MethodLocalType {
        val visitor = object: LocalTypeVisitor<MethodLocalType> {
            override fun visit(type: LocalType.Repetition) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: LocalType.Concatenation) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: LocalType.Choice) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: LocalType.Offer) =
                throw RuntimeException("This function should never be called, since other functions take care of this case.")

            override fun visit(type: LocalType.Initialization) =
                MethodLocalType.Skip

            override fun visit(type: LocalType.Sending) =
                if (targetFuture in analyzedLocalType.preState.getActiveFutures()) {
                    MethodLocalType.Sending(
                        receiver = type.receiver,
                        f = type.f,
                        m = type.m
                    )
                }

                else {
                    MethodLocalType.Skip
                }

            override fun visit(type: LocalType.Receiving) =
                MethodLocalType.Skip

            override fun visit(type: LocalType.Resolution) =
                when (targetFuture) { // This test is equivalent to checking whether future is currently active, since this is ensured by the abstract execution
                    type.f -> MethodLocalType.Resolution(
                        f = type.f,
                        constructor = type.constructor
                    )
                    else -> MethodLocalType.Skip
                }

            override fun visit(type: LocalType.Fetching) =
                if (targetFuture in analyzedLocalType.preState.getActiveFutures()) {
                    MethodLocalType.Fetching(
                        f = type.f,
                        constructor = type.constructor
                    )
                }

                else {
                    MethodLocalType.Skip
                }

            override fun visit(type: LocalType.Suspension) =
                if (type.suspendedFuture == targetFuture) { // This test is equivalent to checking whether future is currently active, since this is ensured by the abstract execution
                    MethodLocalType.Suspension(
                        suspendedFuture = type.suspendedFuture,
                        awaitedFuture = type.awaitedFuture
                    )
                }

                else {
                    MethodLocalType.Skip
                }

            override fun visit(type: LocalType.Reactivation) =
                MethodLocalType.Skip // FIXME ich weiche hier vom Paper ab. Die Projektion im Paper erscheint mir im Moment nicht wirklich sinnvoll

            override fun visit(type: LocalType.Skip) = MethodLocalType.Skip

            override fun visit(type: LocalType.Termination) =
                MethodLocalType.Skip
        }

        return analyzedLocalType.type.accept(visitor)
    }

    override fun visit(type: AnalyzedLocalType.ConcatenationType): MethodLocalType {
        val lhs = project(type.leftAnalyzedType, targetActor, targetFuture)
        val rhs = project(type.rightAnalzedType, targetActor, targetFuture)

        return when {
            lhs is MethodLocalType.Skip -> rhs
            rhs is MethodLocalType.Skip -> lhs
            else -> MethodLocalType.Concatenation(
                lhs = lhs,
                rhs = rhs
            )
        }
    }

    override fun visit(type: AnalyzedLocalType.OfferType): MethodLocalType {
        // Either the future hasn't been created yet...
        return if (targetFuture !in type.preState.getUsedFutures()) {
            val futureCreationBranches =
                type.analyzedBranches.filter {
                    targetFuture in it.postState.getUsedFutures()
                }

            // FIXME: Remove this comment block. We allow this now.
            //if (futureCreationBranches.size > 1) {
            //    throw RuntimeException(
            //        "Future symbols created in different branches may not intersect. " +
            //        "This should have already been checked during abstract type execution, " +
            //        "thus this exception should never happen and is a programming error."
            //    )
            //}
            //val creationBranch = futureCreationBranches.firstOrNull()

            // ...and it is created in some of the branches
            if (futureCreationBranches.isNotEmpty()) {
                // then only project those branches
                val projections = futureCreationBranches
                    .map { project(it, targetActor, targetFuture) }

                // and select the first one for projection, if they are all equivalent.
                val fstProjection = projections.head
                if (projections.tail.all { futureEquivalent(fstProjection, it) }) {
                    fstProjection
                }

                else {
                    throw MethodProjectionException(
                        type = type.type,
                        message = """
                            The future $targetFuture has been created multiple times in different branches.
                            This would be OK, if they all would describe the same behavior, however, this is
                            not the case.
                        """.trimIndent()
                    )
                }
            }

            // ...otherwise the future does not participate in the branching and we can skip it
            else {
                MethodLocalType.Skip
            }
        }

        // ...OR it exists already, in which case it gets more complicated
        else {
            // ... if there is more than 1 branch, check whether all are reading from the choosing future resolved in the branching
            if (type.analyzedBranches.size > 1) {
                val labeledSplit = extractBranchLabels(
                    targetActor,
                    targetFuture,
                    type.getCastedType().f,
                    type
                )

                // if they are, extract the common prefix and label the branches
                if (labeledSplit != null) {
                    (
                        labeledSplit.prefix concat
                        MethodLocalType.Fetching(type.getCastedType().f, null) concat
                        MethodLocalType.Offer(
                            f = type.getCastedType().f,
                            branches = labeledSplit.branches
                        )
                    )!!
                }

                // Otherwise, all branches must be future equivalent, or the type is malformed
                else {
                    val fixed = identicalBranches(targetActor, targetFuture, type.analyzedBranches)

                    fixed
                        ?: throw MethodProjectionException(
                            type = type.type,
                            message =
                                """|Error during method projection.
                                   |Future ${targetFuture.value} of actor $targetActor behaves differently in different branches,
                                   |although it is not the future choosing the branching (which is ${type.getCastedType().f.value})
                                   |or reading from its result or being called by it.
                                   |
                                   |Or it behaves differently between branches before reading the result.
                                   |""".trimMargin()
                        )
                }
            }

            // or there is only 1 branch. In that case, we only project that one
            else if (type.analyzedBranches.size == 1) {
                project(type.analyzedBranches.first(), targetActor, targetFuture)
            }

            // or there arent any branches. We skip the type then
            else {
                MethodLocalType.Skip
            }
        }
    }

    override fun visit(type: AnalyzedLocalType.ChoiceType): MethodLocalType {
        // Either the future hasn't been created yet...
        return if (targetFuture !in type.preState.getUsedFutures()) {
            val futureCreationBranches =
                type.analyzedChoices.filter {
                    targetFuture in it.postState.getUsedFutures()
                }

            if (futureCreationBranches.size > 1) {
                throw RuntimeException(
                    "Future symbols created in different branches may not intersect. " +
                    "This should have already been checked during abstract type execution, " +
                    "thus this exception should never happen and is a programming error."
                )
            }

            val creationBranch = futureCreationBranches.firstOrNull()
            // ...and it is created in one of the branches
            if (creationBranch != null) {
                // then only project that branch
                project(creationBranch, targetActor, targetFuture)
            }

            // ...otherwise the future does not participate in the branching and we can skip it
            else {
                MethodLocalType.Skip
            }
        }

        // ...OR it exists already, in which case we project on every branch
        else {
            val projectedbranches = type.analyzedChoices
                .map { project(it, targetActor, targetFuture) }

            // If it is not a Skip type in one of the branches, keep the branching
            if (projectedbranches.any {it != MethodLocalType.Skip}) {
                MethodLocalType.Choice(
                    choices = projectedbranches
                )
            }

            // ...otherwise it does not participate and we can skip this part of the type
            else {
                MethodLocalType.Skip
            }
        }
    }

    override fun visit(type: AnalyzedLocalType.RepetitionType): MethodLocalType {
        val repeatedTypeProjection =
            project(type.analyzedRepeatedType, targetActor, targetFuture)

        // Has this future been created in the nested type? Then only project the nested type
        return if (
            targetFuture !in type.preState.getUsedFutures() &&
            targetFuture in type.analyzedRepeatedType.postState.getUsedFutures()
        ) {
            repeatedTypeProjection
        }

        else if (targetFuture in type.preState.getActiveFutures()) { // Otherwise project the whole repetition, if future is currently active
            MethodLocalType.Repetition(
                repeatedType = repeatedTypeProjection
            )
        }

        // if we are not active, skip, since a non active future, which is not created in a repetition can not be activated in a repetition,
        // because this requires resolution of another future created before the loop, which is forbidden
        else {
            MethodLocalType.Skip
        }
    }
}