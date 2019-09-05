package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method

// Utils

// FIXME move into global util library
// https://stackoverflow.com/questions/35808022/kotlin-list-tail-function
val <T> List<T>.tail: List<T>
  get() = drop(1)

val <T> List<T>.head: T
  get() = first()

data class LoopsApplication(
    val modifiedLoops: List<LoopDescription>,
    val modifiedProgram: ProgramTree,
    val positionDelta: List<Int>
)

fun applyLoops(loops: List<LoopDescription>, future: Future, subtree: ProgramTree): LoopsApplication =
    when {
        loops.isEmpty() -> LoopsApplication( // ([], subtree, ε)
            modifiedLoops = emptyList(),
            modifiedProgram = subtree, // TODO: Check whether shallow copy is enough
            positionDelta = emptyList()
        )

        else -> {
            val times = loops.head.times
            val excluded = loops.head.excluded

            val recursionResult = applyLoops(loops.tail, future, subtree)

            if (future in excluded) {
                recursionResult.copy(
                    modifiedLoops = listOf(loops.head) + recursionResult.modifiedLoops
                )
            }

            else {
                LoopsApplication(
                    modifiedLoops =
                          listOf(loops.head.copy(excluded = excluded.plus(future)))
                        + recursionResult.modifiedLoops,
                    modifiedProgram = ProgramTree.Loop( times, recursionResult.modifiedProgram),
                    positionDelta = recursionResult.positionDelta.plus(0)
                )
            }
        }
    }


fun excludeFromLoops(loops: List<LoopDescription>, future: Future) =
    loops.map {
        it.copy(
            excluded = it.excluded.plus(future)
        )
    }

fun replace(methods: Map<Pair<Class, Method>, ProgramTree>, positions: Map<Pair<Class, Method>, List<Int>>, replacement: ProgramTree) =
    methods
        .map {
            (methodId, methodBody) ->
                if (methodId in positions) {
                    methodId to replace(methodBody, positions[methodId]!!, replacement)
                }

                else {
                    methodId to methodBody
                }
        }
        .toMap()


fun insertSeedPointsIntoMethods(methods: Map<Pair<Class, Method>, ProgramTree>, methodPositions: Map<Pair<Class, Method>, List<Int>>, excluded: Set<Pair<Class, Method>>, seeds: Int) =
    Pair(
        replace(
            methods - excluded,
            methodPositions,
            ProgramTree.Split(replicate(seeds, ProgramTree.Placeholder))
        ) + methods.filter { (methodId, methodBody) ->  methodId in excluded },
        (0 .. seeds).map {
            seedNo -> methodPositions.map {
                (methodId, position) -> methodId to position + seedNo
            }.toMap()
        }
    )


// Interact

/**
 * Restrictions created by this construct
 *
 * - Calls must always be resolved, before loops or branching closes
 * - a method can only be called once (thus code generation does not need to deal with different behavior per method)
 */

/**
 * ∃(·, f', A, m')∈Calls.
 *     f' ∉ Resolved
 *   ∧ f' ∉ Suspended
 */
fun guardInteract(globalData: GeneratorState, localData: SeedData) =
    localData
        .calls
        .any { call ->
                   !localData.resolvedFutures.contains(call.future)
                && !globalData.suspendedFutures.contains(call.future)
        }

fun ruleInteract(randomSource: RandomSource, globalData: GeneratorState, localData: SeedData, recursionApplicator: RecursionApplicator): GeneratorState {
    val parentCallCandidates = localData
        .calls
        .filter {
               !localData.resolvedFutures.contains(it.future)
            && !globalData.suspendedFutures.contains(it.future)
        }

    // FIXME Check if callCandidates is empty

    val parentCall = randomSource.selectFromList(parentCallCandidates)!! // FIXME throw proper exception

    val caller = parentCall.callee
    val future = randomSource.newFuture()
    val callee = randomSource.selectActor(localData)
    val method = randomSource.newMethod()

    val loopsApplication = applyLoops(
        loops = localData.loops,
        future = parentCall.future,
        subtree = ProgramTree.Split(
            listOf(
                ProgramTree.Call(
                    future = future,
                    callee = callee,
                    method = method
                ),
                ProgramTree.Placeholder,
                ProgramTree.Placeholder
            )
        )
    )

    // FIXME handle null exception
    val inLoopsPosition = localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!.plus(loopsApplication.positionDelta)

    val updatedCalls = localData.calls.plus(Call(
        caller = caller,
        future = future,
        callee = callee,
        method = method
    ))

    val (methodsWithSeeds, methodPositionsPerSeed) = insertSeedPointsIntoMethods(
        globalData.methods,
        localData.methodPositions,
        setOf(Pair(parentCall.callee, parentCall.method)),
        2
    )

    val seed1Data = localData.copy(
        calls = updatedCalls,
        tracePosition = localData.tracePosition.plus(1),
        methodPositions = methodPositionsPerSeed[0].plus(
            listOf(
                Pair(parentCall.callee, parentCall.method) to inLoopsPosition.plus(1),
                Pair(callee, method) to emptyList()
            )
        ),
        loops = excludeFromLoops(loops = loopsApplication.modifiedLoops, future = future)
    ) // N1 = N[Resolved, Calls ∪ {(A, f, B, m)}, inOptional, tracePosition.1, MethodPositions[(A, m'):=callerModifiedPosition.1] ∪ {(B, m, ε}, excludeFromLoops(ModLoops, f), encodeProgram]

    val seed2Data = localData.copy(
        resolvedFutures = localData.resolvedFutures.plus(future),
        calls = updatedCalls,
        tracePosition = localData.tracePosition.plus(3),
        methodPositions = methodPositionsPerSeed[1].plus(
            Pair(parentCall.callee, parentCall.method) to inLoopsPosition.plus(2)
        ),
        loops = loopsApplication.modifiedLoops
    ) // N2 = N[Resolved ∪ {f}, Calls ∪ {(A, f, B, m)}, inOptional, tracePosition.3, MethodPositions[(A, m'):=callerModifiedPosition.2], ModLoops, encodeProgram]

    val programEncoding = if (localData.encodeProgram) {
            replace(
                methodsWithSeeds,
                Pair(parentCall.callee, parentCall.method),
                localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!, // TODO throw exception
                loopsApplication.modifiedProgram
            )
            .plus(Pair(callee, method) to ProgramTree.Placeholder)
        }

        else {
            globalData.methods
        }

    val protocolExtension = ProtocolTree.Split(
            listOf(
                ProtocolTree.Leaf(GlobalType.Interaction(
                    caller = Class("Generated.${caller.value}"),
                    f = future,
                    callee = Class("Generated.${callee.value}"),
                    m = method
                )),
                ProtocolTree.Seed(seed1Data),
                ProtocolTree.Leaf(GlobalType.Resolution(
                    c = Class("Generated.${callee.value}"),
                    f = future
                )),
                ProtocolTree.Seed(seed2Data)
            )
        )

    return globalData.copy(
            usedFutures = globalData.usedFutures.plus(future),
            traces = replace(
                globalData.traces,
                localData.tracePosition,
                TraceTree.Split(listOf(
                    TraceTree.Leaf(
                        actor = callee,
                        fragment = TraceFragment.Invocation(
                            actor = callee,
                            method = method,
                            future = future
                        )
                    ),
                    TraceTree.Placeholder,
                    TraceTree.Placeholder,
                    TraceTree.Placeholder
                ))
            ),
            reactivationPoints = globalData.reactivationPoints.plus(future to localData.tracePosition.plus(2)),
            methods = programEncoding,
            protocol = recursionApplicator.replaceSeed(protocolExtension)
        )
}

// Read

/**
 * ∃(A, f, ·, ·) ∈ Calls.
 *   ∃(·, f', A, ·) in Calls.
 *       f  ∈ Resolved
 *     ∧ f' ∉ Resolved
 *     ∧ f' ∉ Suspended
 */
fun guardRead(globalData: GeneratorState, localData: SeedData) =
    localData
        .calls
        .any { parentCall ->
               parentCall.future !in localData.resolvedFutures
            && parentCall.future !in globalData.suspendedFutures
            && localData.calls.any {potentialSubCall ->
                      potentialSubCall.caller == parentCall.callee
                   && potentialSubCall.future in localData.resolvedFutures
               }
        }

fun ruleRead(randomSource: RandomSource, globalData: GeneratorState, localData: SeedData, recursionApplicator: RecursionApplicator): GeneratorState {
    val parentCallCandidates = localData
        .calls
        .filter {parentCall ->
               parentCall.future !in localData.resolvedFutures
            && parentCall.future !in globalData.suspendedFutures
            && localData.calls.any {potentialSubCall ->
                   potentialSubCall.caller == parentCall.callee
                && potentialSubCall.future in localData.resolvedFutures
            }
        }

    // FIXME Check if callCandidates is empty

    val parentCall = randomSource.selectFromList(parentCallCandidates)!! // FIXME throw proper exception

    val finishedSubCalls = localData.calls.filter {potentialSubCall ->
           potentialSubCall.caller == parentCall.callee
        && potentialSubCall.future in localData.resolvedFutures
    }

    // FIXME Check if finished sub calls are empty

    val finishedSubCall = randomSource.selectFromList(finishedSubCalls)!! // FIXME throw proper exception

    val readingActor = parentCall.callee
    val readFuture = finishedSubCall.future

    val loopsApplication = applyLoops(
        loops = localData.loops,
        future = parentCall.future,
        subtree = ProgramTree.Split(
            listOf(
                ProgramTree.Get(
                    future = readFuture
                ),
                ProgramTree.Placeholder
            )
        )
    )

    // FIXME handle null exception
    val inLoopsPosition = localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!! + loopsApplication.positionDelta

    val seedData = localData.copy(
        methodPositions = localData.methodPositions.plus(
                Pair(parentCall.callee, parentCall.method) to inLoopsPosition.plus(1)
        )
    )

    val programEncoding = if (localData.encodeProgram) {
        replace(
            globalData.methods,
            Pair(parentCall.callee, parentCall.method),
            localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!, // TODO throw exception
            loopsApplication.modifiedProgram
        )
    }

    else {
        globalData.methods
    }

    val protocolExtension = ProtocolTree.Split(
        listOf(
            ProtocolTree.Leaf(GlobalType.Fetching(
                c = Class("Generated.${readingActor.value}"),
                f = readFuture
            )),
            ProtocolTree.Seed(seedData)
        )
    )

    return globalData.copy(
        methods = programEncoding,
        protocol = recursionApplicator.replaceSeed(protocolExtension)
    )
}


// Complete set of rules

data class Rule(
    val guard: (GeneratorState, SeedData) -> Boolean,
    val application: (RandomSource, GeneratorState, SeedData, RecursionApplicator) -> GeneratorState
)

val rules = setOf(
    Rule(
        guard = ::guardInteract,
        application = ::ruleInteract
    ),
    Rule(
        guard = ::guardRead,
        application = ::ruleRead
    )
)


