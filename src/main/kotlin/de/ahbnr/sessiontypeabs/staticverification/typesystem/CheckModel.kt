package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.abstoolsmods.qualifiedClassName
import de.ahbnr.sessiontypeabs.compiler.TypeBuild
import de.ahbnr.sessiontypeabs.dynamicenforcement.codegen.analysis.findChildren
import de.ahbnr.sessiontypeabs.intersperse
import de.ahbnr.sessiontypeabs.staticverification.VerificationConfig
import de.ahbnr.sessiontypeabs.staticverification.typesystem.exceptions.ModelAnalysisException
import org.abs_models.frontend.ast.*

fun checkModel(model: Model, typeBuild: TypeBuild, verificationConfig: VerificationConfig = VerificationConfig()) {
    // well-formed, since input type has already been analyzed

    val sessionType = typeBuild.analyzedProtocol
    val objectTypes = typeBuild.localTypes

    val classDecls = model.decls.filterIsInstance<ClassDecl>().toSet()
    val actorClassesMapping =
        ActorModelMapping(
            classDecls,
            sessionType,
            verificationConfig
        )

    checkMainBlock(
        model.mainBlock,
        sessionType,
        actorClassesMapping,
        verificationConfig
    )

    ensureNoActorInstantiationsOutsideMain(model, typeBuild)

    checkClasses(sessionType, objectTypes, actorClassesMapping)
}

fun ensureNoActorInstantiationsOutsideMain(model: Model, typeBuild: TypeBuild) {
    // There may be no new expressions instantiating an actor class outside of the Main block.

    val maybeInstantiations = model
        .decls
        .flatMap {
            when (it) {
                is ClassDecl -> it.methodsNoTransform.map { it.blockNoTransform }
                else -> emptyList()
            }
        }
        .mapNotNull {
            decl -> decl
                .findChildren<NewExp>()
                .find {newExp ->
                    typeBuild.analyzedProtocol.postState.getParticipants().any{
                        newExp.qualifiedClassName == it.value
                    }
                }
        }

    if (maybeInstantiations.isNotEmpty()) {
        throw ModelAnalysisException(
            """
                Classes implementing actors of a session may not be instantiated outside the main-block.
                However, we found instantiations outside the main-block for the following classes:
                ${maybeInstantiations.map { it.className }.intersperse(", ")}
            """.trimIndent()
        )
    }
}
