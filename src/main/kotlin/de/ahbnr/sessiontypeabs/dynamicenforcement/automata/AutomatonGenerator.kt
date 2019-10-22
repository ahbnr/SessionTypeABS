package de.ahbnr.sessiontypeabs.dynamicenforcement.automata

import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.CondensedTypeVisitor
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method

fun genAutomaton(t: CondensedType) =
    nfaToDfa(
        genAutomaton(
            t,
            Cache()
        )
    )

/**
 * Helper class for the automaton generator functions in this file.
 *
 * It keeps track of already used state and register names.
 * It also tracks which future is supposed to be stored in which register and to which methods a future name is linked.
 */
private data class Cache(
    var nextFreeState: Int = 0,
    var nextFreeRegister: Int = 0,
    var assignedRegisters: MutableMap<Future, Int> = mutableMapOf(),
    var futureToMethod: MutableMap<Future, Method> = mutableMapOf()
) {
    /**
     * Determines the name of the register in which a future of name [f] is supposed to be stored or allocates a new
     * register name otherwise
     */
    fun registerForFuture(f: Future) =
        assignedRegisters.getOrElse(f) {
            val reg = nextFreeRegister++
            assignedRegisters.plusAssign(Pair(f, reg))

            return reg
        }

    /**
     * Allocates the next unused name of a state
     */
    fun nextState() = nextFreeState++

    /**
     * Retrieves the name of the method upon whose invocation [f] was created.
     */
    fun findMethodForFuture(f: Future) = futureToMethod[f]

    /**
     * Stores the name [m] of the method of which the invocation created the future [f].
     *
     * Should be called whenever invocREv is encountered while building an automaton.
     */
    fun saveMethodForFuture(f: Future, m: Method) =
        futureToMethod.plusAssign(Pair(f, m))
}

private fun genAutomaton(t: CondensedType.InvocationRecv, c: Cache): SessionAutomaton {
    val q0 = c.nextState()
    val q1 = c.nextState()
    val r0 = c.registerForFuture(t.f)

    c.saveMethodForFuture(t.f, t.m)

    return SessionAutomaton(
        setOf(q0, q1),
        q0,
        setOf(
            Transition(
                q0,
                TransitionVerb.InvocREv(
                    method = t.m,
                    register = r0,
                    postCondition = t.postCondition
                ),
                q1
            )
        ),
        setOf(q1)
    )
}

private fun genAutomaton(t: CondensedType.Reactivation, c: Cache): SessionAutomaton {
    val q0 = c.nextState()
    val q1 = c.nextState()
    val r0 = c.registerForFuture(t.f)
    val method =
        c.findMethodForFuture(t.f)
            ?:throw IllegalArgumentException("Reactivation of a future which has not been created yet by an invocation.")

    return SessionAutomaton(
        setOf(q0, q1),
        q0,
        setOf(
            Transition(
                q0,
                TransitionVerb.ReactEv(
                    method = method,
                    register = r0
                ),
                q1
            )
        ),
        setOf(q1)
    )
}

private fun genAutomaton(@Suppress("UNUSED_PARAMETER") t: CondensedType.Skip, c: Cache): SessionAutomaton {
    val q0 = c.nextState()

    return SessionAutomaton(
        setOf(q0),
        q0,
        emptySet(),
        setOf(q0)
    )
}

private fun genAutomaton(t: CondensedType.Concatenation, c: Cache): SessionAutomaton {
    val a0 = genAutomaton(t.lhs, c)
    val a1 = genAutomaton(t.rhs, c)

    return a0 concat a1
}

private fun genAutomaton(t: CondensedType.Repetition, c: Cache): SessionAutomaton {
    val a = genAutomaton(t.repeatedType, c)

    return repeatAutomaton(a)
}

private fun genAutomaton(t: CondensedType.Branching, c: Cache): SessionAutomaton {
    // Return a single state automaton if there are no types to choose from in this branching
    if (t.choices.isEmpty()) {
        val q0 = c.nextState()

        return SessionAutomaton(
            setOf(q0),
            q0,
            emptySet(),
            setOf(q0)
        )
    }

    else {
        val head = t.choices.first()
        val tail = t.choices.drop(1)

        // Construct an automaton for the first branch and recurse on the other ones.
        val headAutomaton = genAutomaton(head, c)
        val tailAutomaton = genAutomaton(
            CondensedType.Branching(tail),
            c
        )

        return headAutomaton union tailAutomaton
    }
}

private fun genAutomaton(t: CondensedType, c: Cache) =
    t.accept(object: CondensedTypeVisitor<SessionAutomaton> {
        override fun visit(type: CondensedType.Reactivation) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.InvocationRecv) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Concatenation) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Repetition) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Branching) =
            genAutomaton(type, c)
        override fun visit(type: CondensedType.Skip) =
            genAutomaton(type, c)
    })

