package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.execute
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.types.condenseType
import de.ahbnr.sessiontypeabs.preprocessing.projection.project
import de.ahbnr.sessiontypeabs.staticverification.resolveActorNames
import de.ahbnr.sessiontypeabs.types.parser.parseGlobalType
import org.abs_models.frontend.ast.Model
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
fun buildTypes(typeSourceFileNames: Iterable<String>, model: Model?) =
    buildTypes(
        parseTypes(typeSourceFileNames),
        model
    )

// TODO KDoc
fun buildTypes(globalTypes: Collection<GlobalType>, model: Model?): TypeBuildCollection {
    val typeBuilds = globalTypes
        .map {globalType ->
            // if not fully qualified names have been used in a type, we first have to resolve them to fully qualified
            // names using the given model:
            val resolvedGlobalType =
                if (model != null) {
                    resolveActorNames(
                        model,
                        globalType
                    )
                }

                else {
                    globalType
                }

            // validate all global session types
            val analysis = execute(CombinedAnalysis(), resolvedGlobalType)

            // Project session types onto actors (classes)
            val objectProjection = project(analysis)

            // For transformation into SessionAutomata, the Local Session Types must
            // first be "condensed"
            val condensedTypes = objectProjection
                .mapValues { (c, localType) -> condenseType(localType.type) }

            TypeBuild(
                analyzedProtocol = analysis,
                localTypes = objectProjection,
                condensedTypes = condensedTypes
            )
        }

    // No actor may participate in more than 1 protocol (global session type)
    ensureProtocolsAreDisjunct(typeBuilds.map(TypeBuild::analyzedProtocol))

    return TypeBuildCollection(
        typeBuilds = typeBuilds
    )
}

/**
 * If there are multiple Global Session Types, multiple
 * maps for local Session Types are generated during projection.
 * This function combines them into one map.
 *
 * //TODO: Throw exception, if a class key is present in multiple maps
 */
fun mergeLocalTypes(localTypesList: List<Map<Class, AnalyzedLocalType>>) =
    localTypesList.fold(emptyMap<Class, AnalyzedLocalType>()) {
            acc, nextElement -> acc + nextElement
    }
