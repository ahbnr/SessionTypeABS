package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.antlr.*
import de.ahbnr.sessiontypeabs.types.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun parseGlobalType(fileName: String): GlobalType {
    val input = CharStreams.fromFileName(fileName)
    val lexer = GlobalTypesLexer(input)
    val parser = GlobalTypesParser(CommonTokenStream(lexer))

    val typeVisitor = object: GlobalTypesBaseVisitor<GlobalType>() {
        override fun visitInitialization(ctx: GlobalTypesParser.InitializationContext) =
            GlobalType.Initialization(
                f = Future(ctx.init().future.text),
                c = Class(ctx.init().classId.text),
                m = Method(ctx.init().method.text)
            )

        override fun visitInteraction(ctx: GlobalTypesParser.InteractionContext) =
            GlobalType.Interaction(
                caller = Class(ctx.interact().caller.text),
                f = Future(ctx.interact().future.text),
                callee = Class(ctx.interact().callee.text),
                m = Method(ctx.interact().method.text)
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
                c = Class(ctx.fetch().classId.text),
                f = Future(ctx.fetch().future.text)
            )

        override fun visitResolution(ctx: GlobalTypesParser.ResolutionContext) =
            GlobalType.Resolution(
                c = Class(ctx.resolve().classId.text),
                f = Future(ctx.resolve().future.text)
            )

        override fun visitRelease(ctx: GlobalTypesParser.ReleaseContext) =
            GlobalType.Release(
                c = Class(ctx.releaseR().classId.text),
                f = Future(ctx.releaseR().future.text)
            )

        override fun visitBranching(ctx: GlobalTypesParser.BranchingContext) =
            GlobalType.Branching(
                c = Class(ctx.branch().classId.text),
                branches = ctx.branch().globalType().map { subCtx -> subCtx.accept(this) }
            )
    }

    return(parser.globalType().accept(typeVisitor))
}
