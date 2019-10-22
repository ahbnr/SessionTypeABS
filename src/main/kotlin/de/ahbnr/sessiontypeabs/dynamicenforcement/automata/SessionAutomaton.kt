package de.ahbnr.sessiontypeabs.dynamicenforcement.automata

import de.ahbnr.sessiontypeabs.types.Method
import org.abs_models.frontend.ast.PureExp

// data TransitionVerb = Epsilon | InvocREv String register | ReactEv ...
sealed class TransitionVerb {
    data class InvocREv(
        val method: Method,
        val register: Int,
        val postCondition: PureExp? = null
    ): TransitionVerb() {
        override fun getRegister() = register
    }

    data class ReactEv(
        val method: Method,
        val register: Int
    ): TransitionVerb() {
        override fun getRegister() = register
    }

    object Epsilon: TransitionVerb() {
        override fun getRegister() = null
    }

    abstract fun getRegister(): Int?
}

data class Transition(
    val q1: Int,
    val verb: TransitionVerb,
    val q2: Int
)

data class SessionAutomaton(
    val Q: Set<Int>,
    val q0: Int,
    val Delta: Set<Transition>,
    val finalStates: Set<Int>
) {
    val registers: Set<Int> by lazy {
        Delta.mapNotNull { it.verb.getRegister() }.toSet()
    }

    fun transitionsOfState(state: Int) =
        Delta
            .filter{t -> t.q1 == state}
            .toSet()

    fun transitionsLeadingIntoState(state: Int) =
        Delta
            .filter{t -> t.q2 == state}
            .toSet()

    fun affectedMethods() =
        Delta
            .map{t -> t.verb}
            .filterIsInstance<TransitionVerb.InvocREv>()
            .map{v -> v.method}
            .toSet()

    fun transitionsForMethod(method: Method) =
        Delta
            .filter{t ->
                when (t.verb) {
                    is TransitionVerb.InvocREv -> t.verb.method == method
                    is TransitionVerb.ReactEv -> t.verb.method == method
                    is TransitionVerb.Epsilon -> false
                }
            }
            .toSet()
}
