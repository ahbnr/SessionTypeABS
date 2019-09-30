package de.ahbnr.sessiontypeabs.types.analysis

import com.sun.org.apache.xalan.internal.xsltc.compiler.CompilerException
import de.ahbnr.sessiontypeabs.head
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.intersperse
import org.abs_models.frontend.ast.ClassDecl
import org.abs_models.frontend.ast.Model

fun resolveActorNames(model: Model, globalType: GlobalType): GlobalType {
    val simpleNamesToClassDecls = model
        .decls
        .filterIsInstance<ClassDecl>()
        .groupBy(ClassDecl::getName)

    return resolveActorNames(simpleNamesToClassDecls, globalType)
}

private fun simpleToQualifiedActor(simpleNamesToClassDecls: Map<String, List<ClassDecl>>, actor: Class): Class {
    val sameNameClassDecls = simpleNamesToClassDecls[actor.value]

    return Class(
        when {
            sameNameClassDecls.isNullOrEmpty() -> actor.value
            sameNameClassDecls.size > 1 -> throw CompilerException(
                """
                        |There are multiple classes in the model with a fitting name for actor ${actor.value}:
                        |${sameNameClassDecls.map { it.qualifiedName }.intersperse(", ")}
                        |
                        |Please consider using a qualified name in the session type specification instead.
                    """.trimMargin()
            )
            else -> sameNameClassDecls.head.qualifiedName
        }
    )
}

private fun resolveActorNames(simpleNamesToClassDecls: Map<String, List<ClassDecl>>, type: GlobalType): GlobalType =
    when (type) {
        is GlobalType.Initialization ->
            type.copy(c = simpleToQualifiedActor(simpleNamesToClassDecls, type.c))
        is GlobalType.Skip -> type
        is GlobalType.Branching ->
            type.copy(
                choosingActor = simpleToQualifiedActor(simpleNamesToClassDecls, type.choosingActor),
                branches = type
                    .branches
                    .map {
                        resolveActorNames(simpleNamesToClassDecls, it)
                    }
            )
        is GlobalType.Repetition ->
            type.copy(
                repeatedType = resolveActorNames(simpleNamesToClassDecls, type.repeatedType)
            )
        is GlobalType.Release ->
            type.copy(
                c = simpleToQualifiedActor(simpleNamesToClassDecls, type.c)
            )
        is GlobalType.Fetching ->
            type.copy(
                c = simpleToQualifiedActor(simpleNamesToClassDecls, type.c)
            )
        is GlobalType.Concatenation ->
            type.copy(
                lhs = resolveActorNames(simpleNamesToClassDecls, type.lhs),
                rhs = resolveActorNames(simpleNamesToClassDecls, type.rhs)
            )
        is GlobalType.Resolution ->
            type.copy(
                c = simpleToQualifiedActor(simpleNamesToClassDecls, type.c)
            )
        is GlobalType.Interaction ->
            type.copy(
                caller = simpleToQualifiedActor(simpleNamesToClassDecls, type.caller),
                callee = simpleToQualifiedActor(simpleNamesToClassDecls, type.callee)
            )
    }
