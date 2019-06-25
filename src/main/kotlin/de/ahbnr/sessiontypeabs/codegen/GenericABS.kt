package de.ahbnr.sessiontypeabs.codegen

import org.abs_models.frontend.ast.*

/**
 * This file provides helper function to build ABS AST nodes for constructs which are commonly used in ABS
 */

// Types
fun processT() =
    DataTypeUse("ABS.Scheduler.Process", List())

// TODO Use fully qualified names everywhere

fun maybeT(param: TypeUse) =
    ParametricDataTypeUse(
        "Maybe",
        List(), //Annotations
        List(param)
    )

fun listT(param: TypeUse) =
    ParametricDataTypeUse(
        "List",
        List(), // Annotations
        List(param)
    )

fun intT() =
    DataTypeUse( // TODO: Using UnknownTypeUse would probably be better
        "Int",
        List() // Annotations
    )

// Constructors
fun justC(param: PureExp) =
    DataConstructorExp("Just", List(param))

fun nothingC() =
    DataConstructorExp("Nothing", List())

fun trueC() =
    DataConstructorExp("True", List())

fun falseC() =
    DataConstructorExp("False", List())

fun nilC() =
    DataConstructorExp("Nil", List())

fun setC(vararg elements: PureExp) =
    FnApp(
        "set",
        List(
            if (elements.isEmpty()) {
                nilC()
            }

            else {
                ListLiteral(
                    List(*elements)
                )
            }
        )
    )

// Common Functions

// On Lists
fun head(param: PureExp) =
    FnApp("head", List(param))

fun filter(f: ParFnAppParam, lst: PureExp) =
    callHigherOrderFun(
        "filter",
        f,
        lst
    )

// Function Declarations
fun funDecl(returnType: TypeUse, name: String, params: org.abs_models.frontend.ast.List<ParamDecl>, def: PureExp) =
    FunctionDecl(
        name,
        List(), // Annotations
        returnType,
        params,
        ExpFunctionDef(def)
    )

fun lambdaDecl(params: org.abs_models.frontend.ast.List<ParamDecl>, def: PureExp) =
    AnonymousFunctionDecl(
        params,
        def
    )

// Other utilities
fun callHigherOrderFun(f: String, fparam: ParFnAppParam, vararg params: PureExp) =
    ParFnApp(
        f,
        List(*params),
        List(
            fparam
        )
    )

fun ifThen(condition: PureExp, then: Stmt) =
    IfStmt(
        List(), // no annotations
        condition,
        if (then is Block) {
            then
        }
        else {
            Block(
                List(), // no annotations
                List(then)
            )
        },
        Opt()
    )
