package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.LocalType
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.condenseType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.analysis.execute
import de.ahbnr.sessiontypeabs.types.analysis.project
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import java.io.File
import java.io.FileInputStream

fun parseTypes(typeSourceFileNames: Iterable<String>): List<GlobalType> =
    typeSourceFileNames
        .map{ fileName ->
            val file = File(fileName)
            val inputStream = FileInputStream(file)

            parseGlobalType(inputStream, file.path)
        }

/**
 * Parses global Session Types, validates them, projects them onto the
 * participating actors and condenses the resulting local Session Types
 */
fun buildTypes(typeSourceFileNames: Iterable<String>) =
    buildTypes(
        parseTypes(typeSourceFileNames)
    )

// TODO KDoc
fun buildTypes(globalTypes: List<GlobalType>): TypeBuild {
    // validate all global session types
    val analysis = globalTypes
        .map { gtype -> execute(CombinedDomain(), gtype) }

    // No actor may participate in more than 1 protocol (global session type)
    ensureProtocolsAreDisjunct(analysis)

    // Project session types onto actors (classes)
    val projections = analysis.map { project(it) }
    // Merge into one map object
    val localTypes = mergeLocalTypes(projections)

    // For transformation into SessionAutomata, the Local Session Types must
    // first be "condensed"
    val condensedTypes = localTypes
        .map { (c, localType) -> c to condenseType(localType) }
        .toMap()

    return TypeBuild(
        analyzedProtocols = analysis,
        localTypes = localTypes,
        condensedTypes = condensedTypes
    )
}

data class TypeBuild(
    val analyzedProtocols: List<AnalyzedGlobalType<CombinedDomain>>,
    val localTypes: Map<Class, LocalType>,
    val condensedTypes: Map<Class, CondensedType>
)

/**
 * If there are multiple Global Session Types, multiple
 * maps for local Session Types are generated during projection.
 * This function combines them into one map.
 *
 * //FIXME: Throw exception, if a class key is present in multiple maps
 */
fun mergeLocalTypes(localTypesList: List<Map<Class, LocalType>>) =
    localTypesList.fold(emptyMap<Class, LocalType>()) {
            acc, nextElement -> acc.plus(nextElement)
    }
