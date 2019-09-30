package de.ahbnr.sessiontypeabs.compiler

import de.ahbnr.sessiontypeabs.mergeMaps
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.CondensedType
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain

data class TypeBuild(
    val analyzedProtocol: AnalyzedGlobalType<CombinedDomain>,
    val localTypes: Map<Class, AnalyzedLocalType>,
    val condensedTypes: Map<Class, CondensedType>
)

class TypeBuildCollection(
    val typeBuilds: List<TypeBuild>
) {
    fun mergedCondensedTypes(): Map<Class, CondensedType> =
        typeBuilds
            .map(TypeBuild::condensedTypes)
            .mergeMaps()

    fun mergedLocalTypes(): Map<Class, AnalyzedLocalType> =
        typeBuilds
            .map(TypeBuild::localTypes)
            .mergeMaps()

    fun mergedGlobalTypes(): Collection<AnalyzedGlobalType<CombinedDomain>> =
        typeBuilds
            .map(TypeBuild::analyzedProtocol)

    // Merge into one map object
    //val localTypes = mergeLocalTypes(projections)
}
