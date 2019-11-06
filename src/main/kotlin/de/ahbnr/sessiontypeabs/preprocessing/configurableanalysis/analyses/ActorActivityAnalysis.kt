package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.MergeException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.SelfContainednessException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.throwNotAtomic
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType

data class ActorActivityAnalysis(
    private val actorStates: Map<Class, ActivityStatus> = emptyMap()
): ConfigurableAnalysis<ActorActivityAnalysis> {
    override fun transfer(t: GlobalType) =
        when (t) {
            is GlobalType.Initialization -> {
                ensureInactive(t.c, t)

                this.copy(
                    actorStates = actorStates + (t.c to ActivityStatus.Active(
                        t.f
                    ))
                )
            }
            is GlobalType.Interaction -> {
                ensureActive(t.caller, t)

                when (val calleeState = getState(t.callee)) {
                    is ActivityStatus.Inactive ->
                        this.copy(
                            actorStates = actorStates +
                                (t.callee to ActivityStatus.Active(
                                    t.f
                                ))
                        )
                    is ActivityStatus.Suspended ->
                        this.copy(
                            actorStates = actorStates +
                                (t.callee to ActivityStatus.Active(
                                    t.f,
                                    calleeState.waitingFutures
                                ))
                        )
                    is ActivityStatus.Active ->
                        throw TransferException(
                            type = t,
                            message = """
                                Callee ${t.callee.value} of an interaction is active, but it must be inactive or
                                suspended at this point to be callable.
                            """
                        )
                }
            }
            is GlobalType.Resolution -> {
                ensureToNotReactivateActives(t.f, t)

                resolveActor(t.c, t.f, t)
                    .reactivateActorsWaitingOnFuture(t.f)
            }
            is GlobalType.Fetching -> {
                ensureActive(t.c, t)

                this.copy()
            }
            is GlobalType.Release -> {
                val actorState = getState(t.c)

                if (actorState is ActivityStatus.Active) {
                    val maybeWaitingFuture = actorState.isWaitingFor(t.f)

                    if (maybeWaitingFuture == null) {
                        this.copy(
                            actorStates = actorStates + (t.c to ActivityStatus.Suspended(
                                actorState.waitingFutures + AwaitingPair(
                                    actorState.activeFuture,
                                    t.f
                                )
                            ))
                        )
                    }

                    else {
                        throw TransferException(
                            type = t,
                            message = """
                                Future ${maybeWaitingFuture.awaitingFuture.value} of actor ${t.c.value} is already
                                waiting for future ${t.f.value}.
                                No two futures of the same actor may await the same future.
                                Therefore this action is not permitted.
                            """.trimIndent()
                        )
                    }
                }

                else {
                    throw TransferException(
                        type = t,
                        message = "In order to read from a future, actor ${t.c.value} must be active at this point, but it is not."
                    )
                }
            }
            is GlobalType.Skip -> this.copy()
            else -> throwNotAtomic(t)
        }

    override fun merge(rhs: ActorActivityAnalysis, branchingContext: GlobalType.Branching) =
        if (this.actorStates == rhs.actorStates) {
            this.copy()
        }

        else {
            throw MergeException(
                type = branchingContext,
                message = """
                    Branching types are only valid if we can be certain for every actor and future, whether it is
                    active, inactive or suspended after the branching.
                    However, this is not the case, since the activity status of some actors/futures differs
                    at the end of some branches.
                """.trimIndent()
            )
        }

    override fun selfContained(preState: ActorActivityAnalysis, context: GlobalType) {
        if (actorStates != preState.actorStates) {
            throw SelfContainednessException(
                type = context,
                message = """
                    Futures must be active at the end of a repeated type if they were active at its beginning and
                    suspended if they were suspended at its beginning.
                    This also means that a future suspended at its beginning can not be reactivated within the type.
                    The activity state of some actors differs at the end of a repeated type, likely because of one of
                    the above reasons.
                    Therefore, this type is invalid.
                """.trimIndent()
            )
        }
    }

    override fun closeScopes(preState: ActorActivityAnalysis, context: GlobalType) =
        this.copy()

    fun getState(actor: Class) =
        actorStates.getOrDefault(actor,
            ActivityStatus.Inactive
        )

    private fun ensureActive(actor: Class, context: GlobalType) {
        if (getState(actor) !is ActivityStatus.Active) {
            throw TransferException(
                type = context,
                message = "Actor ${actor.value} should be active at this point, but it is not."
            )
        }
    }

    private fun ensureInactive(actor: Class, context: GlobalType) {
        if (getState(actor) != ActivityStatus.Inactive) {
            throw TransferException(
                type = context,
                message = "Actor ${actor.value} should be inactive at this point, but it is not."
            )
        }
    }

    private fun ensureToNotReactivateActives(resolvedFuture: Future, context: GlobalType) {
        val activeActorWaitingOnResolvedFuture = actorStates.entries.find {
                (someActor, state) ->
            state is ActivityStatus.Active && state.waitingFutures.any {
                it.awaitedFuture == resolvedFuture
            }
        }

        if (activeActorWaitingOnResolvedFuture != null) {
            throw TransferException(
                type = context,
                message = """
                    Actor ${activeActorWaitingOnResolvedFuture.key.value} is awaiting future
                    ${resolvedFuture.value} which is to be resolved.
                    However, resolving ${resolvedFuture.value} is no valid action here, since
                    ${activeActorWaitingOnResolvedFuture.key.value} is still active and can thus not
                    reactivate its waiting future.
                    Either resolve ${resolvedFuture.value} later, or suspend or resolve the future active on
                    ${activeActorWaitingOnResolvedFuture.key.value} first.
                """.trimIndent()
            )
        }
    }

    private fun resolveActor(resolvingActor: Class, resolvedFuture: Future, context: GlobalType): ActorActivityAnalysis {
        val actorState = getState(resolvingActor)

        if (actorState is ActivityStatus.Active) {
            if (actorState.activeFuture == resolvedFuture) {
                val newActorStates = if (actorState.waitingFutures.isEmpty()) {
                    this.actorStates - resolvingActor
                } else {
                    this.actorStates + (resolvingActor to ActivityStatus.Suspended(
                        actorState.waitingFutures
                    ))
                }

                return this.copy(
                    actorStates = newActorStates
                )
            }

            else {
                throw TransferException(
                    type = context,
                    message = """
                        Actor ${resolvingActor.value} is active on future ${actorState.activeFuture.value} not on
                        ${resolvedFuture.value} so it can not resolve ${resolvedFuture.value}.
                    """.trimIndent()
                )
            }
        }

        else {
            throw TransferException(
                type = context,
                message = """
                    Actor ${resolvingActor.value} must be active to resolve future ${resolvedFuture.value}, but it is not.
                """.trimIndent()
            )
        }
    }

    private fun reactivateActorsWaitingOnFuture(resolvedFuture: Future): ActorActivityAnalysis {
        val reactivations = actorStates.entries.mapNotNull {
                (actor, state) ->
            if (state is ActivityStatus.Suspended) {
                state
                    .isWaitingFor(resolvedFuture)
                    ?.let {
                            awaitingPair ->
                                actor to ActivityStatus.Active(
                                    activeFuture = awaitingPair.awaitingFuture,
                                    waitingFutures = state.waitingFutures - awaitingPair
                                )
                        }
                }

                else  {
                    null
                }
        }

        return this.copy(
            actorStates = actorStates + reactivations
        )
    }

    fun getSuspensionsOnFuture(f: Future): Collection<SuspensionInfo> =
        actorStates.mapNotNull {
            (actor, status) ->
                if (status is ActivityStatus.Suspended) {
                    val maybeAwaitingPair = status.isWaitingFor(f)

                    maybeAwaitingPair
                        ?.let {
                            SuspensionInfo(
                                actor,
                                it.awaitingFuture
                            )
                        }
                }

                else {
                    null
                }
        }

    fun getActiveFuture(actor: Class): Future? =
        when (val state = getState(actor)) {
            is ActivityStatus.Active ->
                state.activeFuture
            else -> null
        }

    fun getActiveFutures(): Collection<Future> =
        actorStates.mapNotNull {
            (actor, status) ->
                if (status is ActivityStatus.Active) {
                    status.activeFuture
                }

                else {
                    null
                }
        }
}

data class AwaitingPair(
    val awaitingFuture: Future,
    val awaitedFuture: Future
)

sealed class ActivityStatus {
    object Inactive: ActivityStatus()

    data class Active(
        val activeFuture: Future,
        val waitingFutures: Set<AwaitingPair> = emptySet()
    ): ActivityStatus() {
        fun isWaitingFor(f: Future): AwaitingPair? =
            waitingFutures.find { it.awaitedFuture == f}
    }

    data class Suspended(
        val waitingFutures: Set<AwaitingPair>
    ): ActivityStatus() {
        fun isWaitingFor(f: Future): AwaitingPair? =
            waitingFutures.find { it.awaitedFuture == f}
    }
}

data class SuspensionInfo (
    val suspendedClass: Class,
    val futureAfterReactivation: Future
)
