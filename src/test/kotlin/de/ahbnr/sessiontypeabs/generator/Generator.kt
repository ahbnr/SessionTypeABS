package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method

private fun recurse(randomSource: RandomSource, state: GeneratorState) =
    if (randomSource.shouldRecurse()) {
        generatorTransition(randomSource, state)
    }

    else {
        state
    }

fun generatorTransition(randomSource: RandomSource, state: GeneratorState): GeneratorState {
    val applicator = randomSource.selectRecursionApplicator(state)

    if (applicator != null) {
        val rule = randomSource.selectRule(state, applicator.seedData)!! // throw exception instead of !!

        val resultingState = rule.application(randomSource, state, applicator.seedData, applicator)

        return recurse(randomSource, resultingState)
    }

    return state
}

fun generatorTransition(randomSource: RandomSource): GeneratorState {
    val initCaller = Class("0")
    val initFuture = randomSource.newFuture()
    val initCallee = randomSource.newActor()
    val initMethod = randomSource.newMethod()

    val initialProtocol =
        ProtocolTree.Split(listOf(
            ProtocolTree.Leaf(
                GlobalType.Initialization(
                    f = initFuture,
                    c = Class("Generated.${initCallee.value}"), // FIXME: Put module name in external constant
                    m = initMethod
                )
            ),
            ProtocolTree.Seed(
                SeedData(
                    resolvedFutures = emptySet(),
                    suspendedFutures = emptySet(),
                    suspensionPoint = null,
                    calls = setOf(Call(
                        caller = initCaller,
                        future = initFuture,
                        callee = initCallee,
                        method = initMethod
                    )),
                    tracePosition = listOf(1),
                    methodPositions = mapOf(
                        Pair(Class("0"), Method("main")) to listOf(1),
                        Pair(initCallee, initMethod) to emptyList()
                    ),
                    loops = emptyList(),
                    inLoop = false,
                    numOfExecutions = 1,
                    encodeProgram = true
                )
            ),
            ProtocolTree.Leaf(
                GlobalType.Resolution(
                    c = Class("Generated.${initCallee.value}"),
                    f = initFuture
                )
            )
        ))

    val initialState = GeneratorState(
            usedFutures = emptySet(),
            traces = TraceTree.Split(
                listOf(
                    TraceTree.Leaf(
                        actor = initCallee,
                        fragment = TraceFragment.Invocation(
                            actor = initCallee,
                            method = initMethod,
                            future = initFuture
                        )
                    ),
                    TraceTree.Placeholder
                )
            ),
            methods = mapOf(
                Pair(initCaller, Method("main")) to
                    MethodDescription(
                        invocationTimes = 1,
                        body = ProgramTree.Split(
                            listOf(
                                ProgramTree.Init(
                                    future = initFuture,
                                    callee = initCallee,
                                    method = initMethod
                                ),
                                ProgramTree.Placeholder
                            )
                        )
                    ),
                Pair(initCallee, initMethod) to
                    MethodDescription(
                        invocationTimes = 1,
                        body = ProgramTree.Placeholder
                    )
            ),
            protocol = initialProtocol
        )

    return recurse(randomSource, initialState)
}

data class GeneratorResult(
    val model: String,
    val traces: Map<Class, List<TraceFragment>>,
    val protocol: GlobalType
)

fun generate(randomSource: RandomSource): GeneratorResult {
    val finalState = generatorTransition(randomSource)

    return GeneratorResult(
        model = buildModel(finalState.methods),
        traces = finalState
            .actors
            .filter { it != Class("0") }
            .map { it to collapse(it, finalState.traces) }
            .toMap(),
        protocol = collapse(finalState.protocol)!! // TODO proper exception
    )
}
