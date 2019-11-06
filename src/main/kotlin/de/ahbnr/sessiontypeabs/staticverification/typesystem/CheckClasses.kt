package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.AnalyzedLocalType
import org.abs_models.frontend.ast.ClassDecl

fun checkClasses(sessionType: AnalyzedGlobalType<CombinedAnalysis>, objectTypes: Map<Class, AnalyzedLocalType>, actorModelMapping: ActorModelMapping) {
    // The instantiation of actorModelMapping already checked, that there is a class for each participant
    // Projection has also already been conducted, since its result is a required parameter.

    for (participant in sessionType.postState.getParticipants()) {
        val localType = objectTypes[participant]

        if (localType == null) {
            throw RuntimeException(
                "A participant has not been projected before attempting to analyze its model implementation. This should never happen and is a programmer error."
            )
        }

        else {
            checkClass(
                actorModelMapping.findClassByActor(participant),
                localType,
                actorModelMapping
            )
        }
    }
}

fun checkClass(classDecl: ClassDecl, objectType: AnalyzedLocalType, actorModelMapping: ActorModelMapping) =
    classDecl
        .methodsNoTransform
        .forEach {
            //FIXME: Make sure, there is no init block or run method.
            checkMethod(
                classDecl,
                it,
                objectType,
                actorModelMapping
            )
        }
