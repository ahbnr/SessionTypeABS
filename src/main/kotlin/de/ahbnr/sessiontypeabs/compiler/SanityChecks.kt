package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.astmods.ModificationLog
import de.ahbnr.sessiontypeabs.compiler.exceptions.CompilerException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.types.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.Class

/**
 * @throws CompilerException iff there is a class participating in the given
 *                          global Session Types, but which was not found during
 *                          modification of the ABS model.
 */
fun checkForMissingParticipants(protocols: Collection<AnalyzedGlobalType<CombinedAnalysis>>, modLog: ModificationLog) {
    val expectedParticipants = protocols
        .fold(emptySet<Class>()) {
                acc, nextProtocol -> acc union nextProtocol.postState.getParticipants()
        }

    val actualParticipants= modLog.modifiedClasses.map{ Class(it.qualifiedName) }
    val difference = expectedParticipants.minus(actualParticipants)

    if (difference.isNotEmpty()) {
        // TODO use better exception type
        throw CompilerException("The following classes are participating in the protocol, but could not be found in the supplied ABS model: ${difference.map{it.value}}")
    }
}


/**
 * @throws CompilerException iff there is an actor (class) which participates in
 *                           more than one global session type.
 */
fun ensureProtocolsAreDisjunct(analyzedGlobalTypes: List<AnalyzedGlobalType<CombinedAnalysis>>) {
    val participants = analyzedGlobalTypes.map { it.postState.getParticipants() }

    for ((outerIndex, participantSet) in participants.withIndex()) {
        for ((innerIndex, otherParticipantSet) in participants.withIndex()) {
            if (outerIndex != innerIndex) { // Dont compare a set to itself
                val intersection = participantSet intersect otherParticipantSet
                if (intersection.isNotEmpty()) {
                    throw CompilerException("If multiple protocols are used, no class may participate in more than 1 protocol. The following classes are participating in more than 1 protocol.") // TODO: Better exception
                }
            }
        }
    }
}
