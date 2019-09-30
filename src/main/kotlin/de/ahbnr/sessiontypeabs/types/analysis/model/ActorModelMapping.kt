package de.ahbnr.sessiontypeabs.types.analysis.model

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.analysis.AnalyzedGlobalType
import de.ahbnr.sessiontypeabs.types.analysis.domains.CombinedDomain
import de.ahbnr.sessiontypeabs.types.analysis.exceptions.ModelAnalysisException
import de.ahbnr.sessiontypeabs.types.intersperse
import org.abs_models.frontend.ast.ClassDecl
import org.abs_models.frontend.typechecker.InterfaceType
import org.abs_models.frontend.typechecker.Type
import org.abs_models.frontend.typechecker.UnionType

class ActorModelMapping {
    private val actorsToClasses: Map<Class, ClassDecl>
    private val typesToActors: Map<Type, Class>

    constructor(
        classes: Iterable<ClassDecl>,
        sessionType: AnalyzedGlobalType<CombinedDomain>
    ) {
        actorsToClasses =
            sessionType
                .postState
                .getParticipants()
                .associateWith { actorIdentifier ->
                    classes.find { classDecl -> classDecl.qualifiedName == actorIdentifier.value }
                        ?: throw ModelAnalysisException("There is no class in the model representing actor ${actorIdentifier.value}.")
                }

        val actorsToInterfaceTypes =
            actorsToClasses
                .mapValues {
                        (_, classDecl) ->
                    classDecl
                        .implementedInterfaceUsesNoTransform
                        // capture entire interface hierarchy
                        .flatMap { (it.type as InterfaceType).decl.superTypes.map{ superInterface -> superInterface.type} + it.type}
                }

        // No two interfaces of different actors may intersect, otherwise we cant statically differentiate them for object values
        for ((actor1, interfaceTypes1) in actorsToInterfaceTypes) {
            for ((actor2, interfaceTypes2) in actorsToInterfaceTypes - actor1) {
                val interfacesIntersection = (interfaceTypes1 intersect interfaceTypes2).filter { it.qualifiedName != "ABS.StdLib.Object" }

                if (interfacesIntersection.isNotEmpty()) {

                    throw ModelAnalysisException("""
                        |Classes implementing actors of a Session Type may not share interfaces or super interfaces except for ABS.StdLib.Object.
                        |
                        |Actors ${actor1.value} and ${actor2.value} share the following interfaces:
                        |${interfacesIntersection.map { it.qualifiedName }.intersperse(", ")}
                    """.trimMargin())
                }
            }
        }

        typesToActors =
            actorsToInterfaceTypes
                .map { (actor, interfaces) ->
                    interfaces.associateWith { actor }
                }
                .fold(emptyMap(), {acc, next -> acc + next})
    }

    fun findActorByType(absType: Type): Class? {
        if (absType is UnionType) {
            for (interfaceType in absType.types) {
                val maybeActor = findActorByType(interfaceType)

                if (maybeActor != null) {
                    return maybeActor
                }
            }

            return null
        }

        else {
            return typesToActors[absType]
        }
    }
    fun findClassByActor(actor: Class): ClassDecl = actorsToClasses[actor]
        ?: throw RuntimeException("Could not retrieve ABS model class for the actor $actor. Since this should have been checked before, this should never happen and is a programmer error.")
}
