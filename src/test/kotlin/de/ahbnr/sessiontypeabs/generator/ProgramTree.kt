package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method

sealed class ProgramTree {
    data class Loop(
        val times: Int,
        val subtree: ProgramTree
    ) : ProgramTree()

    data class Init(
        val future: Future,
        val callee: Class,
        val method: Method
    ) : ProgramTree()

    data class Call(
        val future: Future,
        val callee: Class,
        val method: Method
    ) : ProgramTree()

    data class Await(
        val future: Future
    ) : ProgramTree()

    data class Get(
        val future: Future
    ) : ProgramTree()

    data class Split(
        val subtrees: List<ProgramTree>
    ) : ProgramTree()

    object Placeholder: ProgramTree()
}

fun replace(programTree: ProgramTree, locations: List<Int>, replacement: ProgramTree): ProgramTree =
    when (programTree) {
        // FIXME throw exception in first two cases, if locations empty
        is ProgramTree.Loop ->
            programTree.copy(subtree = replace(programTree.subtree, locations.tail, replacement))
        is ProgramTree.Split ->
            programTree.copy(subtrees = programTree.subtrees.replaced(
                locations.head,
                replace(programTree.subtrees[locations.head], locations.tail, replacement)
            ))
        else -> replacement // TODO throw exception, if not placeholder or locations not empty
    }

fun replace(methods: Map<Pair<Class, Method>, MethodDescription>, methodId: Pair<Class, Method>, position: List<Int>, replacement: ProgramTree) =
    methods.plus(
        methodId to methods[methodId]!!.copy(
            body = replace(methods[methodId]!!.body, position, replacement) // TODO exception if id not present
        )
    )

// TODO move to utility library

fun List<String>.intersperse(divider: String) =
    if (isEmpty()) {
        ""
    }

    else {
        reduce{ acc, next -> "$acc$divider$next"}
    }

// Collapsing

fun buildTraceStore() =
"""String traceStore = "";"""

fun buildTraceStorePrintMethodDecl() = """Unit printTrace();"""
fun buildTraceStorePrintMethod(methods: Map<Method, MethodDescription>) = """Unit printTrace() {
${checkCompletionCounters(methods).prependIndent("    ")}
    
    println(this.traceStore);
}"""

// FIXME add method name to MethodDescription and remove then unnecessary passing around of Method values
fun buildCompletionCounters(methods: Iterable<Method>) = methods.map { "Int ${it.value}Complete = 0;" }.intersperse("\n")
fun increaseCompletionCounter(method: Method) = "this.${method.value}Complete = this.${method.value}Complete + 1;"
fun checkCompletionCounters(methods: Map<Method, MethodDescription>) = "await ${methods.map {
        (methodName, methodDescription) -> "this.${methodName.value}Complete == ${methodDescription.invocationTimes}"
}.intersperse(" && ")};"

fun buildFutureIdStore() =
"""Map<Fut<Any>, Int> futStore = map[];
Int futCounter = 0;"""

fun registerThisDestiny() = """this.futStore = insert(this.futStore, Pair(thisDestiny, this.futCounter));
this.futCounter = this.futCounter + 1;
"""

fun retrieveDestinyString() = "toString(lookupUnsafe(this.futStore, thisDestiny))"

fun announceInvocation(callee: Class, method: Method) =
"""${registerThisDestiny()}
this.traceStore = this.traceStore + "[TRACE] Invocation " + ${retrieveDestinyString()} + " ${callee.value} ${method.value}\n";"""

fun announceReactivation(callee: Class, method: Method) =
"""this.traceStore = this.traceStore + "[TRACE] Reactivation " + ${retrieveDestinyString()} + " ${callee.value} ${method.value}\n";"""

fun buildProgram(callee: Class, method: Method, programTree: ProgramTree, uniqueIdPrefix: String = "", uniqueIdCounter: Int = 0): String =
    when (programTree) {
        is ProgramTree.Placeholder -> ""
        is ProgramTree.Call -> "Fut<Int> ${programTree.future.value} = fromJust(this.${buildActorFieldName(programTree.callee)})!${programTree.method.value}();"
        is ProgramTree.Init -> "${buildActorFieldName(programTree.callee)}.${programTree.method.value}();"
        is ProgramTree.Await ->
"""await ${programTree.future.value}?;
${announceReactivation(callee, method)}"""

        is ProgramTree.Get -> "${programTree.future.value}.get;"
        is ProgramTree.Split ->
            programTree
                .subtrees
                .mapIndexed { idx, subtree -> buildProgram(callee, method, subtree, "$uniqueIdPrefix$idx")}
                .filter { it.isNotEmpty() }
                .intersperse("\n")
        is ProgramTree.Loop -> {
            val loopedStmts = buildProgram(callee, method, programTree.subtree, uniqueIdPrefix, uniqueIdCounter + 1)

            if (loopedStmts.isEmpty()) {
                ""
            }

            else {
                val counterName = "i$uniqueIdPrefix$uniqueIdCounter"

                """Int $counterName = 0;
while ($counterName < ${programTree.times}) {
${loopedStmts.prependIndent("    ")}

    $counterName = $counterName + 1;
}"""
            }
        }
    }

fun buildMethodDeclaration(methodName: Method) = "Int ${methodName.value}();"

fun buildMethod(actor: Class, methodName: Method, methodDescription: MethodDescription) =
"""Int ${methodName.value}() {
${announceInvocation(actor, methodName).prependIndent("    ")}

${buildProgram(actor, methodName, methodDescription.body).prependIndent("    ")}

${increaseCompletionCounter(methodName).prependIndent("    ")}

    return 0;
}"""

fun buildInitMethodParameters(actors: Set<Class>): String =
    actors
        .map {
            "${buildActorFieldName(it)}"
        }
        .intersperse(", ")

fun buildInitMethodParameterDeclaration(actors: Set<Class>): String =
    actors
        .map {
            "${it.value}I ${buildActorFieldName(it)}"
        }
        .intersperse(", ")

fun buildInitMethod(actors: Set<Class>): String =
"""Unit init(${buildInitMethodParameterDeclaration(actors)}) {
${
    actors
        .map { "this.${buildActorFieldName(it)} = Just(${buildActorFieldName(it)});" }
        .intersperse("\n")
        .prependIndent("    ")
}
}"""

fun buildInitMethodDeclaration(actors: Set<Class>) =
    "Unit init(${buildInitMethodParameterDeclaration(actors)});"

fun buildActorFieldName(actor: Class) = actor.value.toLowerCase()

fun buildInitFields(actors: Set<Class>) =
    actors
        .map { "Maybe<${it.value}I> ${buildActorFieldName(it)} = Nothing;" }
        .intersperse("\n")

fun buildInterface(actors: Set<Class>, actor: Class, methods: Map<Method, MethodDescription>): String =
"""interface ${actor.value}I {
${
  methods.map { (methodName, _) ->
      buildMethodDeclaration(methodName)
  }
      .intersperse("\n")
      .prependIndent("    ")
}

${buildInitMethodDeclaration(actors).prependIndent("    ")}

${buildTraceStorePrintMethodDecl().prependIndent("    ")}
}"""

fun buildInterfaces(actors: Set<Class>, methods: Map<Pair<Class, Method>, MethodDescription>): String =
    methods
        .entries
        .groupBy{ it.key.first }
        .map { (actor, entries) ->
            buildInterface(
                actors,
                actor,
                entries.map { it.key.second to it.value }.toMap()
            )
        }
        .intersperse("\n\n")

fun buildClass(actors: Set<Class>, actor: Class, methods: Map<Method, MethodDescription>): String =
"""class ${actor.value} implements ${actor.value}I {
${buildInitFields(actors).prependIndent("    ")}

${buildFutureIdStore().prependIndent("    ")}

${buildCompletionCounters(methods.keys).prependIndent("    ")}

${buildTraceStore().prependIndent("    ")}

${buildInitMethod(actors).prependIndent("    ")}

${buildTraceStorePrintMethod(methods).prependIndent("    ")}

${
    methods.map { (methodName, methodDescription) ->
        buildMethod(actor, methodName, methodDescription)
    }
        .intersperse("\n\n")
        .prependIndent("    ")
}
}"""

fun buildClasses(actors: Set<Class>, methods: Map<Pair<Class, Method>, MethodDescription>): String =
    methods
        .entries
        .groupBy{ it.key.first }
        .map { (actor, entries) ->
            buildClass(
                actors,
                actor,
                entries.map { it.key.second to it.value }.toMap()
            )
        }
        .intersperse("\n\n")

fun buildMainMethod(mainProgram: MethodDescription, actors: Set<Class>) =
"""{
${
    actors
        .map { "${it.value}I ${buildActorFieldName(it)} = new ${it.value}();" }
        .intersperse("\n")
        .prependIndent("    ")
}

${
    actors
        .map {"${buildActorFieldName(it)}.init(${buildInitMethodParameters(actors)});" }
        .intersperse("\n")
        .prependIndent("    ")
}

${buildProgram(Class("0"), Method("main"), mainProgram.body).prependIndent("    ")}

    // synchronously output traces, to prevent interleaving of output
${
actors
    .map {"${buildActorFieldName(it)}.printTrace();" }
    .intersperse("\n")
    .prependIndent("    ")
}
}"""

fun buildModel(methods: Map<Pair<Class, Method>, MethodDescription>): String {
    // FIXME solve first 2 statements with partitioning
    val mainProgram = methods.entries.find { it.key.first == Class("0") }!!.value // TODO throw exception
    val methodsWithoutMain = methods.filter { it.key.first != Class("0") }
    val actorsWithoutMain = methodsWithoutMain.map { it.key.first }.toSet()

    return """module Generated;

${buildInterfaces(actorsWithoutMain, methodsWithoutMain)}

${buildClasses(actorsWithoutMain, methodsWithoutMain)}

${buildMainMethod(mainProgram, actorsWithoutMain)}"""
}

