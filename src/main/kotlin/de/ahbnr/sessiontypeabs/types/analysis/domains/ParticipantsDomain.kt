package de.ahbnr.sessiontypeabs.types.analysis.domains

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.GlobalTypeVisitor
import de.ahbnr.sessiontypeabs.types.analysis.Mergeable
import de.ahbnr.sessiontypeabs.types.analysis.Repeatable
import de.ahbnr.sessiontypeabs.types.analysis.TransferException
import de.ahbnr.sessiontypeabs.types.analysis.Transferable

// Records the classes participating in the protocol
data class ParticipantsDomain(
    val participants: Set<Class> = emptySet()
): Mergeable<ParticipantsDomain>, Transferable<GlobalType, ParticipantsDomain>, Repeatable<ParticipantsDomain>
{
    override fun loopContained(beforeLoop: ParticipantsDomain, errorDescriptions: MutableList<String>): Boolean {
        beforeLoop.participants.forEach {
            if (!participants.contains(it)) {
                errorDescriptions.add("Class ${it.value} was participating in the protocol before the loop, but not anymore after the 1st iteration.")

                return false
            }
        }

        return true
    }

    private fun introduceClass(c: Class) =
        this.copy(participants = participants.plus(c))

    override fun transfer(label: GlobalType): ParticipantsDomain {
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
        }

        return label.accept(visitor)
    }

    override fun merge(rhs: ParticipantsDomain) =
        this.copy(
            participants = participants union rhs.participants
        )
}