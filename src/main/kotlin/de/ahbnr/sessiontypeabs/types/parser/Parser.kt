package de.ahbnr.sessiontypeabs.types.parser

import de.ahbnr.sessiontypeabs.antlr.*
import de.ahbnr.sessiontypeabs.types.*
import org.abs_models.frontend.ast.*
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import java.io.InputStream
import java.lang.RuntimeException
import org.abs_models.frontend.ast.FnApp
import org.abs_models.frontend.ast.PureExp
import org.abs_models.frontend.ast.ListLiteral
import org.abs_models.frontend.ast.DataConstructorExp

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
            if (ctx.init().mainBlockSym.text == "0") {
                GlobalType.Initialization(
                    f = Future(ctx.init().future.text),
                    c = Class(ctx.init().classId.text),
                    m = Method(ctx.init().method.text),
                    postCondition = ctx.init().postcondition?.let { parsePostCondition(it, parser, fileName) },
                    fileContext = buildFileContext(ctx)
                )
            }

            else {
                throw ParserException(
                    fileName = fileName,
                    line = ctx.init().mainBlockSym.line,
                    column = ctx.init().mainBlockSym.charPositionInLine,
                    message = "The first symbol of a session type file must always be \"0\" representing the Main-Block."
                )
            }

        override fun visitInteraction(ctx: GlobalTypesParser.InteractionContext) =
            GlobalType.Interaction(
                caller = Class(ctx.interact().caller.text),
                f = Future(ctx.interact().future.text),
                callee = Class(ctx.interact().callee.text),
                m = Method(ctx.interact().method.text),
                postCondition = ctx.interact().postcondition?.let { parsePostCondition(it, parser, fileName) },
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
                constructor = ctx.fetch().constructor?.text?.let{ADTConstructor(it)},
                fileContext = buildFileContext(ctx)
            )

        override fun visitResolution(ctx: GlobalTypesParser.ResolutionContext) =
            GlobalType.Resolution(
                c = Class(ctx.resolve().classId.text),
                f = Future(ctx.resolve().future.text),
                constructor = ctx.resolve().constructor?.text?.let{ADTConstructor(it)},
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
                choosingActor = Class(ctx.branch().classId.text),
                branches = ctx.branch().globalType().map { subCtx -> subCtx.accept(this) },
                fileContext = buildFileContext(ctx)
            )

        override fun visitSkipping(ctx: GlobalTypesParser.SkippingContext) =
            GlobalType.Skip
    }

    try {
        val parsedTree = parser.start()
        val parsedType = parsedTree.globalType().accept(typeVisitor)

        if (parsedType.head !is GlobalType.Initialization) {
            throw ParserException(
                fileName = fileName,
                line = parsedTree.start.line,
                column = parsedTree.start.charPositionInLine,
                message = """
                    A global type must begin with an initial call like this: ${
                        GlobalType.Initialization(
                            f = Future("f"),
                            c = Class("P"),
                            m = Method("m")
                        )
                    }
                """.trimIndent()
            )
        }

        return parsedType
    }

    catch (exception: ParseCancellationException) {
        throw parserExceptionFromCancellation(
            exception = exception,
            parser = parser,
            fileName = fileName
        )
    }
}



/**
 * Parse a ABS string literal.
 *
 * This function is an almost identical clone of org.abs_models.frontend.antlr.parser.CreateJastAddASTListener.makeStringLiteral,
 * since the function is private in CreateJastAddASTListener and I didn't want to modify the ABS compiler source code
 * too much.
 *
 * It removes the surrounding '\"' marks and interprets \n \r \t escape sequences.
 *
 * @param literal ABS string literal as generated by ANTLR for the STRINGLITERAL token. Must be surrounded by '\"' or this function will return null.
 * @return ABS string literal AST node
 */
private fun parseStringLiteral(literal: String): StringLiteral? {
    // String literals must begin and end with '\"'
    if (literal.length < 2 || literal[0] != '\"' || literal[1] != '\"') {
        return null
    }

    // the following part is almost identical to org.abs_models.frontend.antlr.parser.CreateJastAddASTListener.makeStringLiteral
    val buffer = StringBuffer(literal.length - 2)

    // i = 1..len-2 to skip beginning and ending \" of the stringliteral
    var i = 1;
    while (i < literal.length - 1) {
        val c = literal[i]

        // if we encounter an escape sequence, interpret it
        if (c == '\\') {
            ++i

            when (val nextChar = literal[i]) {
                'n' -> buffer.append('\n')
                'r' -> buffer.append('\r')
                't' -> buffer.append('\t')
                else -> buffer.append(nextChar)
            }
        }

        // otherwise, just add the current character
        else {
            buffer.append(c)
        }
    }

    return StringLiteral(buffer.toString())
}

/**
 * This function is almost identical to code in the ABS compiler, converting an ANTLR tree to a JastAdd AST
 */
private fun parsePostCondition(postCondition: GlobalTypesParser.Pure_expContext, parser: Parser, fileName: String = "<Unknown File>"): PureExp {
    val visitor = object : GlobalTypesBaseVisitor<PureExp>() {
        override fun visitFunctionExp(ctx: GlobalTypesParser.FunctionExpContext): PureExp {
            val parsedParameters : Array<PureExp>? =
                ctx.pure_exp_list()
                    ?.pure_exp()
                    ?.map { it.accept(this) } // parse each pure expression parameter, if there is a list of parameters
                    ?.toTypedArray()

            return FnApp(
                ctx.qualified_identifier().text,
                List(
                    *(parsedParameters ?: arrayOf())
                )
            )
        }

        override fun visitConstructorExp(ctx: GlobalTypesParser.ConstructorExpContext): PureExp {
           val parsedParameters : Array<PureExp>? =
               ctx.pure_exp_list()
                   ?.pure_exp()
                   ?.map { it.accept(this) } // parse each pure expression parameter, if there is a list of parameters
                   ?.toTypedArray()

           return DataConstructorExp(
               ctx.qualified_type_identifier().text,
               List(
                   *(parsedParameters ?: arrayOf())
               )
           )
        }

        override fun visitUnaryExp(ctx: GlobalTypesParser.UnaryExpContext) =
            when {
                ctx.MINUS() != null -> MinusExp(ctx.pure_exp().accept(this))
                ctx.NEGATION() != null -> NegExp(ctx.pure_exp().accept(this))
                else -> throw RuntimeException("An unknown unary operator symbol has been parsed: ${ctx.op.text}. This should never happen, instead ANTLR should have thrown an exception before.")
            }

        override fun visitMultExp(ctx: GlobalTypesParser.MultExpContext) =
            when {
                ctx.DIV() != null -> DivMultExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.MOD() != null -> ModMultExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.MULT() != null -> MultMultExp(ctx.l.accept(this), ctx.r.accept(this))
                else -> throw RuntimeException("An unknown multiplication operator symbol has been parsed: ${ctx.op.text}. This should never happen, instead ANTLR should have thrown an exception before.")
            }

        override fun visitAddExp(ctx: GlobalTypesParser.AddExpContext) =
            when {
                ctx.MINUS() != null -> SubAddExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.PLUS() != null -> AddAddExp(ctx.l.accept(this), ctx.r.accept(this))
                else -> throw RuntimeException("An unknown addition operator symbol has been parsed: ${ctx.op.text}. This should never happen, instead ANTLR should have thrown an exception before.")
            }

        override fun visitGreaterExp(ctx: GlobalTypesParser.GreaterExpContext) =
            when {
                ctx.GT() != null -> GTExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.GTEQ() != null -> GTEQExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.LT() != null -> LTExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.LTEQ() != null -> LTEQExp(ctx.l.accept(this), ctx.r.accept(this))
                else -> throw RuntimeException("An unknown comparison operator symbol has been parsed: ${ctx.op.text}. This should never happen, instead ANTLR should have thrown an exception before.")
            }

        override fun visitEqualExp(ctx: GlobalTypesParser.EqualExpContext) =
            when {
                ctx.EQEQ() != null -> EqExp(ctx.l.accept(this), ctx.r.accept(this))
                ctx.NOTEQ() != null -> NotEqExp(ctx.l.accept(this), ctx.r.accept(this))
                else -> throw RuntimeException("An unknown equality operator symbol has been parsed: ${ctx.op.text}. This should never happen, instead ANTLR should have thrown an exception before.")
            }

        override fun visitAndExp(ctx: GlobalTypesParser.AndExpContext) =
            AndBoolExp(ctx.l.accept(this), ctx.r.accept(this))

        override fun visitOrExp(ctx: GlobalTypesParser.OrExpContext) =
            OrBoolExp(ctx.l.accept(this), ctx.r.accept(this))

        override fun visitVarOrFieldExp(ctx: GlobalTypesParser.VarOrFieldExpContext) =
            if (ctx.var_or_field_ref().childCount > 1) { // more than 1 child means, there is a "this." in front
                FieldUse(ctx.var_or_field_ref().IDENTIFIER().text)
            } else { // otherwise it's a variable use
                VarUse(ctx.var_or_field_ref().IDENTIFIER().text)
            }

        override fun visitFloatExp(ctx: GlobalTypesParser.FloatExpContext) =
            FloatLiteral(ctx.FLOATLITERAL().text)

        override fun visitIntExp(ctx: GlobalTypesParser.IntExpContext) =
            IntLiteral(ctx.INTLITERAL().text)

        override fun visitStringExp(ctx: GlobalTypesParser.StringExpContext) =
            parseStringLiteral(ctx.STRINGLITERAL().text)
                ?: throw RuntimeException("An invalid string literal has been parsed: ${ctx.STRINGLITERAL().text}. This should never happen, instead ANTLR should have thrown an exception before.")

        override fun visitThisExp(ctx: GlobalTypesParser.ThisExpContext) =
            ThisExp()

        override fun visitDestinyExp(ctx: GlobalTypesParser.DestinyExpContext) =
            DestinyExp()

        override fun visitNullExp(ctx: GlobalTypesParser.NullExpContext) =
            NullExp()

        override fun visitParenExp(ctx: GlobalTypesParser.ParenExpContext) =
            ctx.pure_exp().accept(this)
    }

    try {
        return postCondition.accept(visitor)
    }

    catch (exception: ParseCancellationException) {
        throw parserExceptionFromCancellation(
            exception = exception,
            parser = parser,
            fileName = fileName,
            customMessage = "Failed to parse a post-condition within a Session Type specification."
        )
    }
}

private fun parserExceptionFromCancellation(
    exception: ParseCancellationException,
    parser: Parser,
    fileName: String = "<Unknown File>",
    customMessage: String = "Failed to parse Session Types."
) =
    when(val originalException = exception.cause) {
        is RecognitionException -> {
            val vocabulary = parser.vocabulary
            val expectedTokens = originalException
                .expectedTokens
                ?.toArray()
                ?.map { tokenId -> vocabulary.getDisplayName(tokenId) }
                ?: "Could not determine expected tokens."

            ParserException(
                fileName = fileName,
                line = originalException.offendingToken.line,
                column = originalException.offendingToken.charPositionInLine,
                message = "$customMessage\n\nFound: ${originalException.offendingToken.text}.\nExpected one of: $expectedTokens."
            )
        }

        else -> ParserException(
            fileName = fileName,
            line = -1,
            column = -1,
            message = "$customMessage\n\nCould not determine the expected token and location of the error."
        )
    }
