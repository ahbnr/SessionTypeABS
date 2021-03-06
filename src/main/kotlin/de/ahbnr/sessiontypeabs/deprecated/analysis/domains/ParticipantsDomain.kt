package de.ahbnr.sessiontypeabs.deprecated.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.GlobalTypeVisitor
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Finalizable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Mergeable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Repeatable
import de.ahbnr.sessiontypeabs.deprecated.analysis.domains.interfaces.Transferable

/**
 * Records which actors (classes) are participating in the protocol
 * induced by a global session type.
 *
 * A class is participating, iff its fully qualified name is to be found in
 * any part of the session type.
 */
data class ParticipantsDomain(
    val participants: Set<Class> = emptySet()
): Mergeable<ParticipantsDomain>,
    Transferable<GlobalType, ParticipantsDomain>,
    Repeatable<ParticipantsDomain>,
    Finalizable<ParticipantsDomain>
{
    /**
     * A loop is always considered self-contained by this domain.
     * (An exception is only thrown in case of a programmer error)
     */
    override fun selfContained(beforeLoop: ParticipantsDomain, errorDescriptions: MutableList<String>): Boolean {
        beforeLoop.participants.forEach {
            if (!participants.contains(it)) {
                throw RuntimeException("Class ${it.value} was participating in the protocol before the loop, but not anymore after the 1st iteration. This should never happen and it is an error in this program.")
            }
        }

        return true
    }

    private fun introduceClass(c: Class) =
        this.copy(participants = participants.plus(c))

    override fun transferType(label: GlobalType): ParticipantsDomain {
        val self = this

        val visitor = object : GlobalTypeVisitor<ParticipantsDomain> {
            override fun visit(type: GlobalType.Repetition) = self.copy()
            override fun visit(type: GlobalType.Concatenation) = self.copy()
            override fun visit(type: GlobalType.Branching) = self.copy()

            override fun visit(type: GlobalType.Fetching) = introduceClass(type.c)
            override fun visit(type: GlobalType.Resolution) = introduceClass(type.c)
            override fun visit(type: GlobalType.Interaction) = introduceClass(type.caller).introduceClass(type.callee)
            override fun visit(type: GlobalType.Initialization) = introduceClass(type.c)
            override fun visit(type: GlobalType.Release) = introduceClass(type.c)
            override fun visit(type: GlobalType.Skip) = self.copy()
        }

        return label.accept(visitor)
    }

    override fun merge(rhs: ParticipantsDomain) =
        this.copy(
            participants = participants union rhs.participants
        )

    override fun closeScope(finalizedType: GlobalType) = this.copy()
}