package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.MethodBinding
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.MergeException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.throwNotAtomic
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Method

data class FutureFreshnessAnalysis(
    val futureMapping: Map<Future, MethodBinding> = emptyMap(),
    val accessibleFutures: Set<Future> = emptySet()
): ConfigurableAnalysis<FutureFreshnessAnalysis> {
    override fun transfer(t: GlobalType) =
        when (t) {
            is GlobalType.Initialization -> introduceFuture(t.f, t.c, t.m, t)
            is GlobalType.Interaction -> introduceFuture(t.f, t.callee, t.m, t)
            is GlobalType.Resolution -> ensureIsAccessible(t.f, t)
            is GlobalType.Fetching -> ensureIsAccessible(t.f, t)
            is GlobalType.Release -> ensureIsAccessible(t.f, t)
            is GlobalType.Skip -> this.copy()
            else -> throwNotAtomic(t)
        }

    override fun merge(rhs: FutureFreshnessAnalysis, branchingContext: GlobalType.Branching): FutureFreshnessAnalysis {
        val sharedFutures = this.futureMapping.keys intersect rhs.futureMapping.keys

        val sameFutureDifferentTarget = sharedFutures.find{
            this.futureMapping[it] != rhs.futureMapping[it]
        }

        if (sameFutureDifferentTarget != null) {
            throw MergeException(
                type = branchingContext,
                message = """|Duplicate future names are allowed in different branches, however, they must target the
                             |same actor and method.
                             |
                             |Future ${sameFutureDifferentTarget.value} violates this requirement.
                             |It targets ${this.futureMapping[sameFutureDifferentTarget]} and
                             |${rhs.futureMapping[sameFutureDifferentTarget]} in different branches.
                             |""".trimMargin()
            )
        }

        else {
            /*
                Since closeScopesP eliminates all newly introduced futures from the second component of the state, the
                second components are identical, and we can just continue with one of them.
             */
            return this.copy(
                futureMapping = this.futureMapping + rhs.futureMapping,
                accessibleFutures =  rhs.accessibleFutures
            )
        }
    }


    /**
     * Self-containedness is not checked by this domain, thus this function only throws an exception when a
     * programming error is detected within Session Type ABS
     */
    override fun selfContained(preState: FutureFreshnessAnalysis, context: GlobalType) {
        preState.accessibleFutures.forEach {
            if (it !in accessibleFutures) {
                throw RuntimeException("Future ${it.value} was accessible before opening the current scope, but not anymore when closing it. This should never happen and is a programming error.")
            }
        }
    }

    override fun closeScopes(preState: FutureFreshnessAnalysis, context: GlobalType) =
        /*
            Whenever a concatenated type ends, all futures which are not in a surrounding scope are no longer
            accessible. Therefore, we need to remove all futures from the second state component, which were not already
            accessible in the prestate:
         */
        this.copy(
            accessibleFutures = preState.accessibleFutures
        )

    /**
     * Marks a future identifier as created.
     * Is invoked when applying the [transfer] relation on initialization and
     * interaction types.
     */
    private fun introduceFuture(newFuture: Future, targetActor: Class, calledMethod: Method, label: GlobalType) =
        if (!isFresh(newFuture)) {
            throw TransferException(
                label,
                "Cannot introduce future ${newFuture.value}, since this future has already been introduced."
            )
        }

        else {
            this.copy(
                futureMapping = this.futureMapping + (newFuture to MethodBinding(
                    actor = targetActor,
                    method = calledMethod
                )),
                accessibleFutures = accessibleFutures + newFuture
            )
        }

    /**
     * A future is considered to be fresh, if its identifier has not yet been
     * created during application of the [transfer] relation on an interaction or initialization .
     */
    private fun isFresh(f: Future) = f !in this.futureMapping.keys

    /**
     * Ensures a future identifier has been created / is accessible in current scope using [introduceFuture] before
     * it is used (in a release etc.).
     */
    private fun ensureIsAccessible(f: Future, label: GlobalType) =
        if (isFresh(f)) {
            throw TransferException(
                label,
                "Can not use future ${f.value} here, since it has not yet been introduced by an interaction or initialization or is out of scope."
            )
        }

        else {
            this.copy()
        }

    fun getNonFreshFutures(): Set<Future> =
        futureMapping.keys

    fun getFuturesToTargetMapping(): Map<Future, MethodBinding> =
        futureMapping
}
