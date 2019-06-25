package de.ahbnr.sessiontypeabs.types.analysis

interface Joinable<T> {
    infix fun join(rhs: T): T
}

interface PartiallyOrdered<T> {
    infix fun isLessOrEqualTo(rhs: T): Boolean
}

infix fun <K,V : Joinable<V>> Map<K,V>.join(rhs: Map<K,V>) =
    (this.asSequence() + rhs.asSequence())
        .distinct()
        .groupBy({ it.key }, { it.value })
        .mapValues {
            (_, values) ->
                values.reduce {
                    lhs, rhs -> lhs join rhs
                }
        }
/*
data class AbstractState(
    private val initialized: Boolean,
    private val classes: Map<Class, ClassState>,
    private val futures: Map<Future, FutureActivityState>,
    private val futureToClass: Map<Future, Class>
): Joinable<AbstractState> {
    fun isInitialized() = initialized

    fun isFresh(f: Future) =
        futures.containsKey(f)

    fun initialize() =
        this.copy(initialized = true)

    fun introduceFuture(c: Class, f: Future): AbstractState {
        if (futures.contains(f)) {
            throw IllegalArgumentException("Future has already been introduced.")
        }

        return this.copy(
            futures = futures.plus(f to FutureActivityState()),
            classes = classes.plus(c to classes.getOrDefault(c, ClassState()).introduceFuture(f)),
            futureToClass = futureToClass.plus(f to c)
        )
    }

    fun releaseFuture(f: Future): AbstractState {
        if (isFresh(f)) {
            throw IllegalArgumentException("This future has not yet been created and can therefore not be released.")
        }

        else if (!mightBeActive(f)) {
            throw IllegalArgumentException("Cannot release future that is not active.")
        }

        else {
            val c = futureToClass.getValue(f)

            return this.copy(
                classes = classes.plus(c to classes.getValue(c).suspend())
            )
        }
    }

    fun resolveFuture(f: Future): AbstractState {
        if (isFresh(f)) {
            throw IllegalArgumentException("This future has not yet been created and can therefore not be resolved.")
        }

        else if (!mightBeActive(f)) {
            throw IllegalArgumentException("Cannot resolve a future that is not active.")
        }

        else {
            val c = futureToClass.getValue(f)

            return this.copy(
                futures = futures.plus(f to futures.getValue(f).resolveFuture()),
                classes = classes.plus(c to classes.getValue(c).finishCurrentFuture())
            )
        }
    }

    /**
     * A class is considered active, if it has at least one active future
     */
    fun mightBeActive(c: Class) =
        classes.containsKey(c)
            && classes.getValue(c).futures.any{
                f -> mightBeActive(f)
            }

    /**
     * A future is considered active, if it has not been resolved and its class is not suspended
     */
    fun mightBeActive(f: Future): Boolean {
        val maybeClass = futureToClass[f]

        // There must be a class associated with the future
        if (maybeClass != null) {
            val cstate = classes.getValue(maybeClass)

            return !mightBeResolved(f)
                && !cstate.mightBeSuspended()
        }

        else {
            return false
        }
    }

    fun mightBeResolved(f: Future) =
        futures.containsKey(f)
            &&  futures.getValue(f).mightBeResolved()

    override infix fun join(rhs: AbstractState) =
        this.copy(
            classes = classes join rhs.classes,
            futures = futures join rhs.futures
        )
}

data class AbstractState(
    val futureFreshness: FutureFreshnessDomain,
    val initialized: Boolean = false
) {
    fun isInitialized() = initialized
    fun initialize() = this.copy(initialized = true)
}

// TODO apply
private fun enforceInitialization(state: AbstractState, label: GlobalType) =
    if (!state.isInitialized()) {
        throw TransferException(state, label, "The protocol must be initialized before any other action.")
    }

    else {

    }

fun transfer(state: AbstractState, label: GlobalType.Initialization) =
    if (state.isInitialized()) {
        throw TransferException(state, label, "Protocol is initialized twice.")
    }

    else {
        state
            .initialize()
            .copy(
                futureFreshness = state.futureFreshness.introduceFuture(label.f)
            )
    }

fun transfer(state: AbstractState, label: GlobalType.Interaction) =
    if (!state.futureFreshness.isFresh(label.f)) {
        throw TransferException(state, label, "Every interaction type requires a fresh, unused future, but ${label.f} has already been used.")
    }

    else {
        state
            .initialize()
            .copy(
                futureFreshness = state.futureFreshness.introduceFuture(label.f)
            )
    }
*/
