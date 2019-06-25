package de.ahbnr.sessiontypeabs

import de.ahbnr.sessiontypeabs.codegen.astmods.*
import de.ahbnr.sessiontypeabs.types.*
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.analyze
import de.ahbnr.sessiontypeabs.types.analysis.condenseType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.analysis.project
import org.abs_models.backend.erlang.ErlangBackend
import org.abs_models.frontend.parser.Main
import java.io.File

import java.util.*

import org.abs_models.frontend.ast.*
import kotlin.RuntimeException

/**
 * Modifies an ABS model, such that it complies to a given set of Local Session Types at runtime and then compiles them
 * to Erlang.
 *
 * Use [de.ahbnr.sessiontypeabs.types.parseFile] to parse the Session Types from files.
 */
fun compile(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>) {
    val model = parseModel(absSourceFileNames)
    val types = parseTypes(typeSourceFileNames)

    applyTypesToModel(model, types)
    checkAndRewriteModel(model)

    modelToErlang(model)
}

fun checkForMissingParticipants(protocols: List<AnalyzedGlobalType<CombinedDomain>>, modLog: ModificationLog) {
    val expectedParticipants = protocols
        .fold(emptySet<Class>()) {
            acc, nextProtocol -> acc union nextProtocol.postState.getParticipants()
        }

    val actualParticipants= modLog.modifiedClasses.map{ Class(it.qualifiedName) }
    val difference = expectedParticipants.minus(actualParticipants)

    if (!difference.isEmpty()) {
        // TODO use better exception type
        throw RuntimeException("The following classes are participating in the protocol, but could not be found in the supplied ABS model: $difference")
    }
}

fun compileGlobalTypes(absSourceFileNames: Iterable<String>, typeSourceFileNames: Iterable<String>) {
    val model = parseModel(absSourceFileNames)
    val globalTypes = parseGlobalTypes(typeSourceFileNames)

    val analysis = globalTypes
        .map{ gtype -> analyze(CombinedDomain(), gtype) }

    ensureProtocolsAreDisjunct(analysis)

    val projections = analysis.map{ project(it) }
    val localTypes = mergeLocalTypes(projections)

    val condensedTypes = localTypes
        .map{ (c, localType) -> c to condenseType(localType) }
        .toMap()

    val modLog = applyTypesToModel(model, condensedTypes)
    checkForMissingParticipants(analysis, modLog)

    checkAndRewriteModel(model)

    modelToErlang(model)
}

fun parseModel(absSourceFileNames: Iterable<String>): Model {
    val helperLib = File(ClassLoader.getSystemClassLoader().getResource("schedulerlib.abs").file)
    val files = absSourceFileNames.map{arg -> File(arg)} + listOf(helperLib)

    return Main.parseFiles(true, files);
}

fun parseTypes(typeSourceFileNames: Iterable<String>): Map<Class, CondensedType> =
    typeSourceFileNames
        .map(::parseFile)
        .fold(emptyMap<Class, CondensedType>()) { // Merge type information from each file
                acc, element -> acc.plus(element)
        }

fun parseGlobalTypes(typeSourceFileNames: Iterable<String>): List<GlobalType> =
    typeSourceFileNames
        .map(::parseGlobalTFile)

fun ensureProtocolsAreDisjunct(analyzedGlobalTypes: List<AnalyzedGlobalType<CombinedDomain>>) {
    val participants = analyzedGlobalTypes.map { it.postState.getParticipants() }

    for ((outerIndex, participantSet) in participants.withIndex()) {
        for ((innerIndex, otherParticipantSet) in participants.withIndex()) {
            if (outerIndex != innerIndex) { // Dont compare a set to itself
                val intersection = participantSet intersect otherParticipantSet
                if (!intersection.isEmpty()) {
                    throw RuntimeException("If multiple protocols are used, no class may participate in more than 1 protocol. The following classes are participating in more than 1 protocol.") // TODO: Better exception
                }
            }
        }
    }
}

fun mergeLocalTypes(localTypesList: List<Map<Class, LocalType>>) =
    localTypesList.fold(emptyMap<Class, LocalType>()) {
        acc, nextElement -> acc.plus(nextElement)
    }


fun applyTypesToModel(model: Model, types: Map<Class, CondensedType>) =
    enforceSessionTypesOnModel(model, types)

fun checkAndRewriteModel(model: Model) {
    val parser = Main()
    parser.analyzeFlattenAndRewriteModel(model)

    if (model.hasParserErrors() || model.hasErrors() || model.hasTypeErrors()) {
        System.out.println("Parsing failed.")
    }
}

fun modelToErlang(model: Model) =
    ErlangBackend().compile(model, File("gen/erl/"), EnumSet.noneOf(ErlangBackend.CompileOptions::class.java))
