package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.*
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable

sealed class ActivityType {
    object Inactive : ActivityType()

    data class Suspended(
        val until: Future,
        val bufferedSuspension: Suspended? = null
    ): ActivityType()

    data class Active(
        val bufferedSuspension: Suspended? = null
    ): ActivityType()
}

private typealias ActivityState = JoinSemiFlatLattice<ActivityType>
private typealias KnownActivityState = JoinSemiFlatLattice.Value<ActivityType>
private typealias UnknownActivityState = JoinSemiFlatLattice.Any<ActivityType>

//enum class ActivityState
//    : Joinable<ActivityState>,
//    PartiallyOrdered<ActivityState> {
//    Inactive,
//    Active,
//    Suspended,
//
//    InactiveOrActive,
//    InactiveOrSuspended,
//    ActiveOrSuspended,
//
//    Any;
//
//    override infix fun isLessOrEqualTo(rhs: ActivityState) =
//        if (rhs == this || rhs == Any) {
//            true
//        }
//
//        else {
//            when (this) {
//                Inactive -> rhs == InactiveOrSuspended || rhs == InactiveOrActive
//                Active -> rhs == ActiveOrSuspended || rhs == InactiveOrActive
//                Suspended -> rhs == ActiveOrSuspended || rhs == InactiveOrSuspended
//                else -> false
//            }
//        }
//
//    override infix fun join(rhs: ActivityState) =
//        when {
//            this isLessOrEqualTo rhs -> rhs
//            rhs isLessOrEqualTo this -> this
//            else -> when (this) {
//                Inactive -> when(rhs) {
//                    Active -> InactiveOrActive
//                    Suspended -> InactiveOrSuspended
//                    else -> Any
//                }
//
//                Active -> when(rhs) {
//                    Inactive -> InactiveOrActive
//                    Suspended -> ActiveOrSuspended
//                    else -> Any
//                }
//
//                Suspended -> when(rhs) {
//                    Inactive -> InactiveOrSuspended
//                    Active -> ActiveOrSuspended
//                    else -> Any
//                }
//
//                else -> Any
//            }
//        }
//}

data class ClassActivityDomain(
    private val classStates: Map<Class, ActivityState> = emptyMap()
): Mergeable<ClassActivityDomain>, Transferable<GlobalType, ClassActivityDomain>, Repeatable<ClassActivityDomain>
{
    override fun loopContained(beforeLoop: ClassActivityDomain, errorDescriptions: MutableList<String>): Boolean {
        val maybeError = areMapsWithDefaultValEqual(
            KnownActivityState(ActivityType.Inactive),
            classStates,
            beforeLoop.classStates
        )

        if (maybeError != null) {
            errorDescriptions.add(
                "Before the loop, class ${maybeError.key.value} had activity state ${maybeError.rval}, afterwards ${maybeError.lval}."
            )

            return false
        }

        return true
    }

    private fun getClassState(c: Class) =
        classStates.getOrDefault(c, KnownActivityState(ActivityType.Inactive))

    private fun updateClassState(c: Class, activityType: ActivityState) =
        this.copy(
            classStates = classStates.plus(
                c to activityType
            )
        )

    private fun transfer(label: GlobalType.Resolution): ClassActivityDomain {
        // Make resolved class inactive or recover its old suspended state
        val classState = getClassState(label.c)

        val update1 = when (classState) {
            is KnownActivityState ->
                when (classState.v) {
                    is ActivityType.Active ->
                        if (classState.v.bufferedSuspension == null) {
                            updateClassState(
                                label.c,
                                KnownActivityState(ActivityType.Inactive)
                            )
                        }

                        else {
                            updateClassState(
                                label.c,
                                KnownActivityState(classState.v.bufferedSuspension)
                            )
                        }
                    else -> throw TransferException(
                        label,
                        "Can not resolve future ${label.f.value} since ${label.c.value} is not active."
                    )
                }
            else -> throw TransferException(
                label,
                "Can not resolve future ${label.f.value} since ${label.c.value} might not be active."
            )
        }

        // Reactivate classes suspended on label.f:
        val update2 =
            this.copy(
                classStates = update1.classStates
                    .map{(otherClass, state) ->
                        Pair(
                            otherClass,
                            if (
                                state is KnownActivityState
                                &&  state.v is ActivityType.Suspended
                                &&  state.v.until == label.f
                            ) {
                                KnownActivityState(ActivityType.Active(state.v.bufferedSuspension))
                            }

                            else {
                                state
                            }
                        )
                    }
                    .toMap()
            )

        return update2;
    }

    private fun transfer(label: GlobalType.Initialization): ClassActivityDomain {
        val classState = getClassState(label.c)

        return when (classState) {
            is KnownActivityState ->
                when (classState.v) {
                    is ActivityType.Inactive -> updateClassState(
                        label.c,
                        KnownActivityState(ActivityType.Active())
                    )

                    else -> throw TransferException(
                        label,
                        "Can not initialize on ${label.c.value}, since it is not inactive."
                    )
                }
            else -> throw TransferException(
                label,
                "Can not initialize on ${label.c.value}, since it might not be inactive."
            )
        }
    }

    private fun transfer(label: GlobalType.Interaction): ClassActivityDomain {
        val callerState = getClassState(label.caller)

        return when (callerState) {
            is KnownActivityState ->
                when (callerState.v) {
                    is ActivityType.Active -> {
                        val calleeState = getClassState(label.callee)

                        when (calleeState) {
                            is KnownActivityState ->
                                when (calleeState.v) {
                                    is ActivityType.Inactive -> updateClassState(
                                        label.callee,
                                        KnownActivityState(ActivityType.Active())
                                    )
                                    is ActivityType.Suspended -> updateClassState(
                                        label.callee,
                                        KnownActivityState(ActivityType.Active(calleeState.v))
                                    )
                                    else -> throw TransferException(
                                        label,
                                        "${label.caller.value} can not call ${label.m.value} on ${label.callee.value}, since ${label.callee.value} is already active."
                                    )
                                }
                            else -> throw TransferException(
                                label,
                                "${label.caller.value} can not call ${label.m.value} on ${label.callee.value}, since ${label.callee.value} might already be active."
                            )
                        }
                    }
                    else -> throw TransferException(
                        label,
                        "${label.caller.value} can not call ${label.m.value} on ${label.callee.value}, since ${label.caller.value} is not active."
                    )
                }

            else -> throw TransferException(
                label,
                "${label.caller.value} can not call ${label.m.value} on ${label.callee.value}, since ${label.caller.value} might not be active."
            )
        }
    }

    private fun transfer(label: GlobalType.Fetching) =
        when (getClassState(label.c)) {
            KnownActivityState(ActivityType.Active()) -> this.copy()
            else -> throw TransferException(
                label,
                "${label.c} can not fetch, since it might not be active."
            )
        }

    private fun transfer(label: GlobalType.Release): ClassActivityDomain {
        val classState = getClassState(label.c)

        return when (classState) {
            is KnownActivityState ->
                when(classState.v) {
                    is ActivityType.Active -> updateClassState(
                        label.c,
                        KnownActivityState(ActivityType.Suspended(label.f, classState.v.bufferedSuspension))
                    )
                    else -> throw TransferException(
                        label,
                        "${label.c.value} can not release control, since it is not active."
                    )
                }
            else -> throw TransferException(
                label,
                "${label.c.value} can not release control, since it might not be active."
            )
        }
    }

    override fun transfer(label: GlobalType) =
        when (label) {
            is GlobalType.Release -> transfer(label)
            is GlobalType.Fetching -> transfer(label)
            is GlobalType.Initialization -> transfer(label)
            is GlobalType.Interaction -> transfer(label)
            is GlobalType.Resolution -> transfer(label)
            else -> this.copy()
        }

    override infix fun merge(rhs: ClassActivityDomain) =
        this.copy(
            classStates = classStates join rhs.classStates
        )

    fun getClassesSuspendedOnFuture(f: Future) =
        classStates
            .map { (c, state) ->
                if (state is KnownActivityState && state.v is ActivityType.Suspended && state.v.until == f) {
                    c
                }

                else {
                    null
                }
            }
            .filterNotNull()

    fun isActive(c: Class): Boolean {
        val classState = getClassState(c)

        return classState is KnownActivityState && classState.v is ActivityType.Active
    }
}