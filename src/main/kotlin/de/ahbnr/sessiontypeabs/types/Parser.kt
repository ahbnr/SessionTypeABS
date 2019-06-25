package de.ahbnr.sessiontypeabs.types

import de.ahbnr.sessiontypeabs.antlr.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * Parses Local Session Types from a file and returns their data representation for each name of a class they have been
 * applied on.
 */
fun parseFile(fileName: String): Map<Class, CondensedType> {
    val input = CharStreams.fromFileName(fileName)
    val lexer = LocalTypesLexer(input)
    val parser = LocalTypesParser(CommonTokenStream(lexer))

    val typeVisitor = object: LocalTypesBaseVisitor<CondensedType>() {
        override fun visitInvocREvT(ctx: LocalTypesParser.InvocREvTContext) =
            CondensedType.InvocationRecv(
                Future(ctx.invocREv().future.text),
                Method(ctx.invocREv().method.text)
            )

        override fun visitRepeatT(ctx: LocalTypesParser.RepeatTContext) =
            CondensedType.Repetition(
                ctx.repeat().repeatedType.accept(this)
            )

        override fun visitConcatT(ctx: LocalTypesParser.ConcatTContext): CondensedType =
            CondensedType.Concatenation(
                ctx.lhs.accept(this),
                ctx.rhs.accept(this)
            )

        override fun visitReactEvT(ctx: LocalTypesParser.ReactEvTContext) =
            CondensedType.Reactivation(Future(ctx.reactEv().future.text))

        override fun visitBranchT(ctx: LocalTypesParser.BranchTContext) =
            CondensedType.Branching(
                ctx.branch().localType().map{subCtx -> subCtx.accept(this)}
            )
    }

    val classToType = parser.typeAssignments().accept(object: LocalTypesBaseVisitor<Map<Class, CondensedType>>() {
        override fun visitTypeAssignments(ctx: LocalTypesParser.TypeAssignmentsContext) =
            ctx
                .typeAssignment()
                .map{
                    Class(it.className.text) to it.localType().accept(typeVisitor)
                }
                .toMap()
    })


    return classToType
}

fun parseGlobalTFile(fileName: String): GlobalType {
    val input = CharStreams.fromFileName(fileName)
    val lexer = GlobalTypesLexer(input)
    val parser = GlobalTypesParser(CommonTokenStream(lexer))

    val typeVisitor = object: GlobalTypesBaseVisitor<GlobalType>() {
        override fun visitInitialization(ctx: GlobalTypesParser.InitializationContext) =
            GlobalType.Initialization(
                f=Future(ctx.init().future.text),
                c=Class(ctx.init().classId.text),
                m=Method(ctx.init().method.text)
            )

        override fun visitInteraction(ctx: GlobalTypesParser.InteractionContext) =
            GlobalType.Interaction(
                caller=Class(ctx.interact().caller.text),
                f=Future(ctx.interact().future.text),
                callee=Class(ctx.interact().callee.text),
                m=Method(ctx.interact().method.text)
            )

        override fun visitRepetition(ctx: GlobalTypesParser.RepetitionContext) =
            GlobalType.Repetition(
                ctx.repeat().repeatedType.accept(this)
            )

        override fun visitConcatenation(ctx: GlobalTypesParser.ConcatenationContext): GlobalType =
            GlobalType.Concatenation(
                ctx.lhs.accept(this),
                ctx.rhs.accept(this)
            )

        override fun visitFetching(ctx: GlobalTypesParser.FetchingContext) =
            GlobalType.Fetching(
                c=Class(ctx.fetch().classId.text),
                f=Future(ctx.fetch().future.text)
            )

        override fun visitResolution(ctx: GlobalTypesParser.ResolutionContext) =
            GlobalType.Resolution(
                c=Class(ctx.resolve().classId.text),
                f=Future(ctx.resolve().future.text)
            )

        override fun visitRelease(ctx: GlobalTypesParser.ReleaseContext) =
            GlobalType.Release(
                c=Class(ctx.releaseR().classId.text),
                f=Future(ctx.releaseR().future.text)
            )

        override fun visitBranching(ctx: GlobalTypesParser.BranchingContext) =
            GlobalType.Branching(
                c=Class(ctx.branch().classId.text),
                branches=ctx.branch().globalType().map{subCtx -> subCtx.accept(this)}
            )
    }

    return(parser.globalType().accept(typeVisitor))
}
