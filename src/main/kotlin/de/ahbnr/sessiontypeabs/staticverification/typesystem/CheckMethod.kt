package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.AnalyzedLocalType
import de.ahbnr.sessiontypeabs.preprocessing.projection.project
import org.abs_models.frontend.ast.ClassDecl
import org.abs_models.frontend.ast.MethodImpl

fun checkMethod(classDecl: ClassDecl, methodImpl: MethodImpl, objectType: AnalyzedLocalType, actorModelMapping: ActorModelMapping) =
    objectType
        .postState
        .getFuturesToTargetMapping()
        .entries
        .filter { (_, target) -> classDecl.qualifiedName == target.actor.value && methodImpl.methodSigNoTransform.name == target.method.value }
        .forEach {
                (future, target) ->
            val methodProjection =
                project(objectType, target.component1(), future)

            // We do not check the receiving type at the start of the projection, since the projection already ensures, that it's there
            checkStmts(
                StmtsEnvironment(
                    Futures = objectType.postState.getNonFreshFutures(),
                    actorModelMapping = actorModelMapping,
                    participants = objectType.postState.getParticipants().map(Class::value)
                ),
                methodImpl.blockNoTransform.stmtsNoTransform.toList(),
                methodProjection
            )
        }

