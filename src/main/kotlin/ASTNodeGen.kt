package session_type_abs

import org.abs_models.frontend.ast.*;

// Types
fun processT() =
  DataTypeUse("Process", List());

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
  DataTypeUse(
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
