package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.antlr.LocalTypesBaseVisitor
import de.ahbnr.sessiontypeabs.antlr.LocalTypesLexer
import de.ahbnr.sessiontypeabs.antlr.LocalTypesParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Parses Local Session Types from a file and returns their data representation for each name of a class they have been
 * applied on.
 */
fun parseFile(fileName: String): Map<String, LocalType> {
    val input = CharStreams.fromFileName(fileName)
    val lexer = LocalTypesLexer(input)
    val parser = LocalTypesParser(CommonTokenStream(lexer))

    val typeVisitor = object: LocalTypesBaseVisitor<LocalType>() {
        override fun visitInvocREvT(ctx: LocalTypesParser.InvocREvTContext) =
            LocalType.InvocationRecv(
                ctx.invocREv().future.text,
                ctx.invocREv().method.text
            )

        override fun visitRepeatT(ctx: LocalTypesParser.RepeatTContext) =
            LocalType.Repetition(
                ctx.repeat().repeatedType.accept(this)
            )

        override fun visitConcatT(ctx: LocalTypesParser.ConcatTContext): LocalType =
            LocalType.Concatenation(
                ctx.lhs.accept(this),
                ctx.rhs.accept(this)
            )

        override fun visitReactEvT(ctx: LocalTypesParser.ReactEvTContext) =
            LocalType.Reactivation(ctx.reactEv().future.text)

        override fun visitBranchT(ctx: LocalTypesParser.BranchTContext) =
            LocalType.Branching(
                ctx.branch().localType().map{subCtx -> subCtx.accept(this)}
            )
    }

    val classToType = parser.typeAssignments().accept(object: LocalTypesBaseVisitor<Map<String, LocalType>>() {
        override fun visitTypeAssignments(ctx: LocalTypesParser.TypeAssignmentsContext) =
            ctx
                .typeAssignment()
                .map{
                    it.className.text to it.localType().accept(typeVisitor)
                }
                .toMap()
    })


    return(classToType)
}
