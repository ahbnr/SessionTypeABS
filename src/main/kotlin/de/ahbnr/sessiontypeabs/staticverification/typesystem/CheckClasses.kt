package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses.CombinedAnalysis
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
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

fun checkClass(classDecl: ClassDecl, objectType: AnalyzedLocalType, actorModelMapping: ActorModelMapping) {
    if (classDecl.hasInitBlock()) {
        throw ModelAnalysisException(
            "Classes implementing an actor of a session may not have a custom init-block. This is violated by class ${classDecl.qualifiedName}."
        )
    }

    else if (classDecl.hasRecoverBranch()) {
        throw ModelAnalysisException(
            "Classes implementing an actor of a session may not have a recovery-block. This is violated by class ${classDecl.qualifiedName}."
        )
    }

    else if (classDecl.allMethodSigs.any { it.name == "run" }) {
        throw ModelAnalysisException(
            "Classes implementing an actor of a session may not have a run-method. This is violated by class ${classDecl.qualifiedName}."
        )
    }

    classDecl
        .methodsNoTransform
        .forEach {
            //FIXME: Make sure, there is no init block or run method, or recovery block.
            checkMethod(
                classDecl,
                it,
                objectType,
                actorModelMapping
            )
        }
}
