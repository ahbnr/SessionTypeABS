package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.*
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable

// We recording a list of active futures, since computation of a future may temporarily suspended
// and during this time, another future can be computed.

private typealias ComputationState = JoinSemiFlatLattice<List<Future>>
private typealias UnknownComputationState = JoinSemiFlatLattice.Any<List<Future>>
private typealias KnownComputationState = JoinSemiFlatLattice.Value<List<Future>>

data class ClassComputationDomain(
    private val classFutures: Map<Class, ComputationState> = emptyMap()
): Mergeable<ClassComputationDomain>, Transferable<GlobalType, ClassComputationDomain>, Repeatable<ClassComputationDomain>
{
    override fun loopContained(beforeLoop: ClassComputationDomain, errorDescriptions: MutableList<String>): Boolean {
        val maybeError = areMapsWithDefaultValEqual(
            KnownComputationState(emptyList()),
            classFutures,
            beforeLoop.classFutures
        )

        if (maybeError != null) {
            errorDescriptions.add(
                "Before the loop, class ${maybeError.key.value} was computing future(s) ${maybeError.rval}, afterwards ${maybeError.lval}."
            )

            return false
        }

        return true
    }

    private fun getFutures(c: Class) =
        classFutures.getOrDefault(c, KnownComputationState(emptyList()))

    private fun updateFutures(c: Class, state: ComputationState) =
        this.copy(
            classFutures = classFutures.plus(
                c to state
            )
        )

    private fun addFuture(c: Class, f: Future): ClassComputationDomain {
        val futures = getFutures(c)

        return when (futures) {
            is UnknownComputationState -> updateFutures(c, futures)
            is KnownComputationState -> updateFutures(
                c,
                KnownComputationState(listOf(f).plus(futures.v))
            )
        }
    }

    private fun transfer(label: GlobalType.Initialization) =
        // NOTE trying to activate an already active class is being handled by ClassActivityDomain
        addFuture(label.c, label.f)

    private fun transfer(label: GlobalType.Interaction) =
        // NOTE trying to activate an already active class is being handled by ClassActivityDomain
        addFuture(label.callee, label.f)

    private fun transfer(label: GlobalType.Resolution): ClassComputationDomain {
        val computedFutures = getFutures(label.c)

        when (computedFutures) {
            is KnownComputationState -> {
                val currentFuture = if (computedFutures.v.isEmpty()) {
                    null
                }

                else {
                    computedFutures.v.first()
                }

                when (currentFuture) {
                    label.f -> return updateFutures(label.c,
                        KnownComputationState(computedFutures.v.drop(1))
                    )
                    null -> throw TransferException(
                        label,
                        "Future ${label.f.value} can not be resolved by ${label.c.value}, since it has no active future."
                    )
                    else -> throw TransferException(
                        label,
                        "Future ${label.f.value} can not be resolved by ${label.c.value}, since it is computing future ${currentFuture.value}."
                    )
                }
            }

            is UnknownComputationState -> throw TransferException(
                label,
                "Future ${label.f.value} can not be resolved by ${label.c.value}, since it might have no active future."
            )
        }
    }

    override fun transfer(label: GlobalType) =
        when (label) {
            is GlobalType.Initialization -> transfer(label)
            is GlobalType.Interaction -> transfer(label)
            is GlobalType.Resolution -> transfer(label)
            else -> this.copy()
        }

    override fun merge(rhs: ClassComputationDomain) =
        this.copy(
            classFutures = classFutures join rhs.classFutures
        )

    fun getCurrentFutureForClass(c: Class): Future? {
        val futures = getFutures(c)

        return when (futures) {
            is KnownComputationState ->
                futures.v.firstOrNull()
            else -> null
        }
    }
}