package de.ahbnr.sessiontypeabs.staticverification.typesystem

import de.ahbnr.sessiontypeabs.compiler.TypeBuild
import de.ahbnr.sessiontypeabs.staticverification.VerificationConfig
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
    checkClasses(sessionType, objectTypes, actorClassesMapping)
}
