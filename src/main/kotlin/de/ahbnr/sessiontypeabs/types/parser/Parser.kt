package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.antlr.*
import de.ahbnr.sessiontypeabs.types.*
import org.abs_models.frontend.antlr.parser.ABSLexer
import org.abs_models.frontend.antlr.parser.ABSParser
import org.abs_models.frontend.antlr.parser.CreateJastAddASTListener
import org.abs_models.frontend.antlr.parser.SyntaxErrorCollector
import org.abs_models.frontend.parser.ASTPreProcessor
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTreeWalker
import java.io.InputStream

fun parsePostCondition(postCondition: String) {
    try {
        //SyntaxErrorCollector errorlistener = new SyntaxErrorCollector(file, raiseExceptions);

        val input = ANTLRInputStream(postCondition)
        val lexer = ABSLexer(input)

        // lexer.removeErrorListeners();
        // lexer.addErrorListener(errorlistener)

        val tokens = CommonTokenStream(lexer)
        val aparser = ABSParser(tokens)

        //aparser.removeErrorListeners();
        //aparser.addErrorListener(errorlistener);

        val tree = aparser.pure_exp()

        //if (errorlistener.parserErrors.isEmpty()) {
            val walker = ParseTreeWalker()
            val l = CreateJastAddASTListener(null) // FIXME correct filename
            walker.walk(l, tree)


            // Preprocessing should have not any effect on pure expressions, but we keep the step just in case
            val u = ASTPreProcessor().preprocess(l.compilationUnit)

            return u.
        //} else {
        //    String path = "<unknown path>";
        //    if (file != null) path = file.getPath();
        //    @SuppressWarnings("rawtypes")
        //    CompilationUnit u = new CompilationUnit(path,new List(),new List(),new List(),new Opt(),new List(),new List(),new List());
        //    u.setParserErrors(errorlistener.parserErrors);
        //    return u;
        //}
    }
}

fun parseGlobalType(inputStream: InputStream, fileName: String = "<Unknown File>"): GlobalType {
    val input = CharStreams.fromStream(inputStream)
    val lexer = GlobalTypesLexer(input)
    val parser = GlobalTypesParser(CommonTokenStream(lexer))
        parser.errorHandler = BailErrorStrategy()

    fun buildFileContext(ctx: GlobalTypesParser.GlobalTypeContext) =
        FileContext(
            startLine = ctx.start.line,
            startColumn = ctx.start.charPositionInLine,
            file = fileName
        )

    val typeVisitor = object: GlobalTypesBaseVisitor<GlobalType>() {
        override fun visitInitialization(ctx: GlobalTypesParser.InitializationContext) =
            GlobalType.Initialization(
                f = Future(ctx.init().future.text),
                c = Class(ctx.init().classId.text),
                m = Method(ctx.init().method.text),
                fileContext = buildFileContext(ctx)
            )

        override fun visitInteraction(ctx: GlobalTypesParser.InteractionContext) =
            GlobalType.Interaction(
                caller = Class(ctx.interact().caller.text),
                f = Future(ctx.interact().future.text),
                callee = Class(ctx.interact().callee.text),
                m = Method(ctx.interact().method.text),
                fileContext = buildFileContext(ctx)
            )

        override fun visitRepetition(ctx: GlobalTypesParser.RepetitionContext) =
            GlobalType.Repetition(
                ctx.repeat().repeatedType.accept(this),
                fileContext = buildFileContext(ctx)
            )

        override fun visitConcatenation(ctx: GlobalTypesParser.ConcatenationContext): GlobalType =
            GlobalType.Concatenation(
                ctx.lhs.accept(this),
                ctx.rhs.accept(this),
                fileContext = buildFileContext(ctx)
            )

        override fun visitFetching(ctx: GlobalTypesParser.FetchingContext) =
            GlobalType.Fetching(
                c = Class(ctx.fetch().classId.text),
                f = Future(ctx.fetch().future.text),
                fileContext = buildFileContext(ctx)
            )

        override fun visitResolution(ctx: GlobalTypesParser.ResolutionContext) =
            GlobalType.Resolution(
                c = Class(ctx.resolve().classId.text),
                f = Future(ctx.resolve().future.text),
                fileContext = buildFileContext(ctx)
            )

        override fun visitRelease(ctx: GlobalTypesParser.ReleaseContext) =
            GlobalType.Release(
                c = Class(ctx.releaseR().classId.text),
                f = Future(ctx.releaseR().future.text),
                fileContext = buildFileContext(ctx)
            )

        override fun visitBranching(ctx: GlobalTypesParser.BranchingContext) =
            GlobalType.Branching(
                c = Class(ctx.branch().classId.text),
                branches = ctx.branch().globalType().map { subCtx -> subCtx.accept(this) },
                fileContext = buildFileContext(ctx)
            )
    }

    try {
        return parser.globalType().accept(typeVisitor)
    }

    catch (exception: ParseCancellationException) {
        val originalException = exception.cause as RecognitionException
        val vocabulary = parser.vocabulary
        val expectedTokens = originalException
            .expectedTokens
            ?.toArray()
            ?.map { tokenId -> vocabulary.getDisplayName(tokenId) }
            ?: "Could not determine expected tokens."

        throw ParserException(
            fileName = fileName,
            line = originalException.offendingToken.line,
            column = originalException.offendingToken.charPositionInLine,
            message = "Failed to parse Session Types.\n\nFound: ${originalException.offendingToken.text}.\nExpected one of: $expectedTokens."
        )
    }
}

