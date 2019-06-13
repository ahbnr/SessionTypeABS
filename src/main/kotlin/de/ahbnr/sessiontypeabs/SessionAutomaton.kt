package de.ahbnr.sessiontypeabs

// data TransitionVerb = InvocREv String register | ReactEv ...
sealed class TransitionVerb {
    class InvocREv(
        val method: String,
        val register: Int
    ): TransitionVerb();

    class ReactEv(
        val method: String,
        val register: Int
    ): TransitionVerb();
}

class Transition(
    val q1: Int,
    val verb: TransitionVerb,
    val q2: Int
)

class SessionAutomaton(
    val Q: Set<Int>,
    val q0: Int,
    val Delta: Set<Transition>,
    val registers: Set<Int>,
    val finalStates: Set<Int>
) {
    fun transitionsForState(state: Int) =
        Delta
            .filter{t -> t.q1 == state}
            .toSet()

    fun affectedMethods() =
        Delta
            .map{t -> t.verb}
            .filterIsInstance<TransitionVerb.InvocREv>()
            .map{v -> v.method}
            .toSet()

    fun transitionsForMethod(method: String) =
        Delta
            .filter{t ->
                when (t.verb) {
                    is TransitionVerb.InvocREv -> t.verb.method == method
                    is TransitionVerb.ReactEv -> t.verb.method == method
                }
            }
            .toSet()
}
