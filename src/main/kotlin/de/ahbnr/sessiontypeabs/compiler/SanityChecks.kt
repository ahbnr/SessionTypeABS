package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.codegen.astmods.ModificationLog
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.Class

/**
 * @throws RuntimeException iff there is a class participating in the given
 *                          global Session Types, but which was not found during
 *                          modification of the ABS model.
 */
fun checkForMissingParticipants(protocols: List<AnalyzedGlobalType<CombinedDomain>>, modLog: ModificationLog) {
    val expectedParticipants = protocols
        .fold(emptySet<Class>()) {
                acc, nextProtocol -> acc union nextProtocol.postState.getParticipants()
        }

    val actualParticipants= modLog.modifiedClasses.map{ Class(it.qualifiedName) }
    val difference = expectedParticipants.minus(actualParticipants)

    if (difference.isNotEmpty()) {
        // TODO use better exception type
        throw RuntimeException("The following classes are participating in the protocol, but could not be found in the supplied ABS model: $difference")
    }
}


/**
 * @throws RuntimeException iff there is an actor (class) which participates in
 *                          more than one global session type.
 */
fun ensureProtocolsAreDisjunct(analyzedGlobalTypes: List<AnalyzedGlobalType<CombinedDomain>>) {
    val participants = analyzedGlobalTypes.map { it.postState.getParticipants() }

    for ((outerIndex, participantSet) in participants.withIndex()) {
        for ((innerIndex, otherParticipantSet) in participants.withIndex()) {
            if (outerIndex != innerIndex) { // Dont compare a set to itself
                val intersection = participantSet intersect otherParticipantSet
                if (intersection.isNotEmpty()) {
                    throw RuntimeException("If multiple protocols are used, no class may participate in more than 1 protocol. The following classes are participating in more than 1 protocol.") // TODO: Better exception
                }
            }
        }
    }
}
