package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.types.analysis.project
import org.abs_models.frontend.ast.ClassDecl
import org.abs_models.frontend.ast.MethodImpl

fun checkMethod(classDecl: ClassDecl, methodImpl: MethodImpl, objectType: AnalyzedLocalType, actorModelMapping: ActorModelMapping) =
    objectType
        .postState
        .getFuturesToTargetMapping()
        .entries
        .filter { (_, target) -> classDecl.qualifiedName == target.component1().value && methodImpl.methodSigNoTransform.name == target.component2().value }
        .forEach {
                (future, target) ->
            val methodProjection = project(objectType, target.component1(), future)

            // We do not check the receiving type at the start of the projection, since the projection already ensures, that it's there
            checkStmts(
                StmtsEnvironment(
                    Futures = objectType.postState.getUsedFutures(),
                    actorModelMapping = actorModelMapping,
                    participants = objectType.postState.getParticipants().map(Class::value)
                ),
                methodImpl.blockNoTransform.stmtsNoTransform.toList(),
                methodProjection
            )
        }

