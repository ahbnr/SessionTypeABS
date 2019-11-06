package de.ahbnr.sessiontypeabs.deprecated.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.*
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.utils.JoinSemiFlatLattice
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.utils.areMapsWithDefaultValEqual
import de.ahbnr.sessiontypeabs.deprecated.analysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.ConfigurableAnalysis

sealed class ActivityType {
    object Inactive : ActivityType()

    data class Suspended(
        val until: Future,
        val bufferedSuspension: Suspended? = null
        // ^There may still be other suspensions which need to be reapplied, after the current future is resolved
    ): ActivityType()

    data class Active(
        val bufferedSuspension: Suspended? = null
    ): ActivityType()
}

private typealias ActivityState = JoinSemiFlatLattice<ActivityType>
private typealias KnownActivityState = JoinSemiFlatLattice.Value<ActivityType>
private typealias UnknownActivityState = JoinSemiFlatLattice.Any<ActivityType>

/**
 * Tracks whether a class is inactive, active or suspended.
 * Also tracks on what future a class is suspended and if there are multiple stacked suspensions.
 */
data class ClassActivityDomain(
    private val classStates: Map<Class, ActivityState> = emptyMap()
): Mergeable<ClassActivityDomain>,
    Transferable<GlobalType, ClassActivityDomain>,
    Repeatable<ClassActivityDomain>,
    Finalizable<ClassActivityDomain>
{
    /**
     * A loop is considered self-contained here, iff the activity state of no class changed.
     */
    override fun selfContained(beforeLoop: ClassActivityDomain, errorDescriptions: MutableList<String>): Boolean {
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

    /**
     * When resolving, the resolving class becomes inactive or goes back into suspended state,
     * if there are still non reactivated suspensions.
     *
     * Furthermore, classes suspended on the resolved future must be reactivated.
     */
    private fun transfer(label: GlobalType.Resolution): ClassActivityDomain {
        // Make resolved class inactive or recover its old suspended state
        val update1 =
            when (val classState = getClassState(label.c)) {
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

        return update2
    }

    /**
     * Whenever the protocol is initialized, the invoked class is activated
     */
    private fun transfer(label: GlobalType.Initialization): ClassActivityDomain =
        when (val classState = getClassState(label.c)) {
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

    /**
     * Whenever an interaction takes place, the callee must be activated.
     *
     * Also it must be checked, whether the caller is active to perform the
     * interaction and the callee must be inactive.
     */
    private fun transfer(label: GlobalType.Interaction): ClassActivityDomain =
        when (val callerState = getClassState(label.caller)) {
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

    /**
     * When fetching, the fetching actor must be confirmed to be active to do so
     */
    private fun transfer(label: GlobalType.Fetching) =
        when (getClassState(label.c)) {
            KnownActivityState(ActivityType.Active()) -> this.copy()
            else -> throw TransferException(
                label,
                "${label.c.value} can not fetch, since it might not be active."
            )
        }

    /**
     * An actor can only release control, if it was active before.
     */
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

    override fun transferType(label: GlobalType) =
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

    /**
     * A class is considered to be active, if one of its methods has been invoked during an
     * initialization or interaction or if it has been reactivated after an suspension
     * and it has not been suspended or the corresponding future been resolved since.
     */
    fun isActive(c: Class): Boolean {
        val classState = getClassState(c)

        return classState is KnownActivityState && classState.v is ActivityType.Active
    }

    override fun closeScope(finalizedType: GlobalType) = this.copy()
}