package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method
import java.lang.RuntimeException

// Utils

// FIXME move into global util library
// https://stackoverflow.com/questions/35808022/kotlin-list-tail-function
val <T> List<T>.tail: List<T>
  get() = drop(1)

val <T> List<T>.head: T
  get() = first()

fun replace(methods: Map<Pair<Class, Method>, MethodDescription>, positions: Map<Pair<Class, Method>, List<Int>>, replacement: ProgramTree) =
    methods
        .map {
            (methodId, methodDescription) ->
                if (methodId in positions) {
                    methodId to methodDescription.copy(
                        body = replace(methodDescription.body, positions[methodId]!!, replacement)
                    )
                }

                else {
                    methodId to methodDescription
                }
        }
        .toMap()


fun insertSeedPointsIntoMethods(methods: Map<Pair<Class, Method>, MethodDescription>, methodPositions: Map<Pair<Class, Method>, List<Int>>, excluded: Set<Pair<Class, Method>>, seeds: Int) =
    Pair(
        replace(
            methods - excluded,
            methodPositions,
            ProgramTree.Split(replicate(seeds, ProgramTree.Placeholder))
        ) + methods.filter { (methodId, _) ->  methodId in excluded },
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
       localData.suspensionPoint == null
    && localData
           .calls
           .any { call ->
                      call.future !in localData.resolvedFutures
                   && call.future !in localData.suspendedFutures
           }

fun ruleInteract(randomSource: RandomSource, globalData: GeneratorState, localData: SeedData, recursionApplicator: RecursionApplicator): GeneratorState {
    val parentCallCandidates = localData
        .calls
        .filter {call ->
               call.future !in localData.resolvedFutures
            && call.future !in localData.suspendedFutures
        }

    // FIXME Check if callCandidates is empty

    val parentCall = randomSource.selectFromList(parentCallCandidates)!! // FIXME throw proper exception

    val caller = parentCall.callee
    val future = randomSource.newFuture()
    val callee = randomSource.selectActor(localData)
    val method = randomSource.newMethod()

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
        3
    )

    val callerMethodPosition = localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!

    val seed1Data = localData.copy(
        calls = updatedCalls,
        tracePosition = localData.tracePosition.plus(1),
        methodPositions = methodPositionsPerSeed[0].plus(
            listOf(
                Pair(parentCall.callee, parentCall.method) to callerMethodPosition + 1,
                Pair(callee, method) to listOf(0)
            )
        )
    ) // N1 = N[Resolved, Calls ∪ {(A, f, B, m)}, inOptional, tracePosition.1, MethodPositions[(A, m'):=callerModifiedPosition.1] ∪ {(B, m, ε}, excludeFromLoops(ModLoops, f), encodeProgram]

    val seed2Data = localData.copy(
        suspensionPoint = SuspensionOpportunity(
            suspendableActor = parentCall.callee,
            suspendableFuture = parentCall.future,
            suspendableMethod = parentCall.method,
            awaitableFuture = future,
            reactivationTracePoint = localData.tracePosition + 3
        ),
        calls = updatedCalls,
        tracePosition = localData.tracePosition.plus(2),
        methodPositions = methodPositionsPerSeed[1].plus(
            listOf(
                Pair(parentCall.callee, parentCall.method) to callerMethodPosition + 2,
                Pair(callee, method) to listOf(1)
            )
        )
    ) // N1 = N[Resolved, Calls ∪ {(A, f, B, m)}, inOptional, tracePosition.1, MethodPositions[(A, m'):=callerModifiedPosition.1] ∪ {(B, m, ε}, excludeFromLoops(ModLoops, f), encodeProgram]

    val seed3Data = localData.copy(
        resolvedFutures = localData.resolvedFutures.plus(future),
        calls = updatedCalls,
        tracePosition = localData.tracePosition.plus(4),
        methodPositions = methodPositionsPerSeed[2].plus(
            Pair(parentCall.callee, parentCall.method) to callerMethodPosition + 3
        )
    ) // N2 = N[Resolved ∪ {f}, Calls ∪ {(A, f, B, m)}, inOptional, tracePosition.3, MethodPositions[(A, m'):=callerModifiedPosition.2], ModLoops, encodeProgram]

    val programEncoding = if (localData.encodeProgram) {
            replace(
                methodsWithSeeds,
                Pair(parentCall.callee, parentCall.method),
                localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!, // TODO throw exception
                ProgramTree.Split(
                    listOf(
                        ProgramTree.Call(
                            future = future,
                            callee = callee,
                            method = method
                        ),
                        ProgramTree.Placeholder, // Directly after call
                        ProgramTree.Placeholder, // Potential suspension point
                        ProgramTree.Placeholder  // After call has been resolved
                    )
                )
            )
            .plus(Pair(callee, method) to MethodDescription(
                invocationTimes = localData.numOfExecutions,
                body = ProgramTree.Split(
                    listOf(
                        ProgramTree.Placeholder, // Start of method
                        ProgramTree.Placeholder  // potential suspension point of caller
                    )
                )
            ))
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
                ProtocolTree.Seed(seed2Data),
                ProtocolTree.Leaf(GlobalType.Resolution(
                    c = Class("Generated.${callee.value}"),
                    f = future
                )),
                ProtocolTree.Seed(seed3Data)
            )
        )

    val result = globalData.copy(
            usedFutures = globalData.usedFutures + future,
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
                    TraceTree.Placeholder, // Directly after call
                    TraceTree.Placeholder, // Potential suspension point
                    TraceTree.Placeholder, // Potential reactivation point
                    TraceTree.Placeholder  // After call has been resolved
                ))
            ),
            methods = programEncoding,
            protocol = recursionApplicator.replaceSeed(protocolExtension)
        )

    // Force the insertion of a suspension on the created future, if we are currently in a loop.
    // This and the fact, that no method can be called twice outside of loops seems to make violations of linearity / ordered causality unlikely / impossible.
    // But ultimately we need to find a reliable solution
    return if (localData.inLoop) {
        val suspendRecursionApplicator = result.protocol.recursionPoints().find { it.seedData == seed2Data }
            ?: throw RuntimeException(
                "Couldn't find the recursion point for inserting a suspension during QuickCheck data generation." +
                "This should never happen, because the recursion point in question has just been generated."
            )

        ruleSuspend(randomSource, result, seed2Data, suspendRecursionApplicator)
    }

    else {
        result
    }
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
       localData.suspensionPoint == null
    && localData
           .calls
           .any { parentCall ->
                  parentCall.future !in localData.resolvedFutures
               && parentCall.future !in localData.suspendedFutures
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
            && parentCall.future !in localData.suspendedFutures
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

    // FIXME handle null exception
    val parentCallPosition = localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!

    val seedData = localData.copy(
        methodPositions = localData.methodPositions.plus(
                Pair(parentCall.callee, parentCall.method) to parentCallPosition + 1
        )
    )

    val programEncoding = if (localData.encodeProgram) {
        replace(
            globalData.methods,
            Pair(parentCall.callee, parentCall.method),
            localData.methodPositions[Pair(parentCall.callee, parentCall.method)]!!, // TODO throw exception
            ProgramTree.Split(
                listOf(
                    ProgramTree.Get(
                        future = readFuture
                    ),
                    ProgramTree.Placeholder
                )
            )
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

// Suspend

/**
 * ∃(·, f, A, ·) ∈ Calls.
 *   ∃(A, f', ·, ·) ∈ Calls.
 *       f, f' ∉ Resolved
 *     ∧ f ∉ Suspended
 *     ∧ ¬inOptional
 */
fun guardSuspend(globalData: GeneratorState, localData: SeedData) =
    localData.suspensionPoint != null

fun ruleSuspend(randomSource: RandomSource, globalData: GeneratorState, localData: SeedData, recursionApplicator: RecursionApplicator): GeneratorState {
    // FIXME Check if suspension point is null, though this should be impossible
    val suspensionPoint = localData.suspensionPoint!!

    // Loops can be ignored, impossible that loop is not applied at this point

    val seedData = localData.copy(
        suspensionPoint = null,
        suspendedFutures = localData.suspendedFutures + suspensionPoint.suspendableFuture,
        methodPositions = localData.methodPositions.plus(
            Pair(suspensionPoint.suspendableActor, suspensionPoint.suspendableMethod)
                    to
                localData.methodPositions[Pair(suspensionPoint.suspendableActor, suspensionPoint.suspendableMethod)]!!.plus(1)
        )
    )

    val programEncoding = if (localData.encodeProgram) {
        replace(
            globalData.methods,
            Pair(suspensionPoint.suspendableActor, suspensionPoint.suspendableMethod),
            localData.methodPositions[Pair(suspensionPoint.suspendableActor, suspensionPoint.suspendableMethod)]!!, // TODO throw exception
            ProgramTree.Split(
                listOf(
                    ProgramTree.Await(
                        future = suspensionPoint.awaitableFuture
                    ),
                    ProgramTree.Placeholder
                )
            )
        )
    }

    else {
        globalData.methods
    }

    val protocolExtension = ProtocolTree.Split(
        listOf(
            ProtocolTree.Leaf(GlobalType.Release(
                c = Class("Generated.${suspensionPoint.suspendableActor.value}"),
                f = suspensionPoint.awaitableFuture
            )),
            ProtocolTree.Seed(seedData)
        )
    )

    return globalData.copy(
        traces = replace(
            globalData.traces,
            suspensionPoint.reactivationTracePoint,
            TraceTree.Leaf(
                actor = suspensionPoint.suspendableActor,
                fragment = TraceFragment.Reactivation(
                    actor = suspensionPoint.suspendableActor,
                    method = suspensionPoint.suspendableMethod,
                    future = suspensionPoint.suspendableFuture
                )
            )
        ),
        methods = programEncoding,
        protocol = recursionApplicator.replaceSeed(protocolExtension)
    )
}

// Loops

fun guardLoop(globalData: GeneratorState, localData: SeedData) =
    localData.suspensionPoint == null

fun ruleLoop(randomSource: RandomSource, globalData: GeneratorState, localData: SeedData, recursionApplicator: RecursionApplicator): GeneratorState {
    val loopDescription = randomSource.newLoopDescription()

    val seed1Data = localData.copy(
        tracePosition = localData.tracePosition + 0 + 0,
        methodPositions = localData.methodPositions.map {
            (methodId, position) -> methodId to position + 0 + 0
        }.toMap(),
        numOfExecutions = localData.numOfExecutions * loopDescription.times,
        inLoop = true
    )

    val seed2Data = localData.copy(
        tracePosition = localData.tracePosition + 1,
        methodPositions = localData.methodPositions.map {
            (methodId, position) -> methodId to position + 1
        }.toMap()
    )

    val protocolExtension = ProtocolTree.Split(
        listOf(
            ProtocolTree.Repetition(
                subtree = ProtocolTree.Seed(seed1Data)
            ),
            ProtocolTree.Seed(seed2Data)
        )
    )

    val methodSubtree = ProgramTree.Split(
        listOf(
            ProgramTree.Loop(
                loopDescription.times,
                ProgramTree.Placeholder
            ),
            ProgramTree.Placeholder
        )
    )

    val programEncoding = if (localData.encodeProgram) {
        replace(
            globalData.methods,
            localData.methodPositions,
            methodSubtree
        )
    }

    else {
        globalData.methods
    }

    return globalData.copy(
        traces = replace(
            globalData.traces,
            localData.tracePosition,
            TraceTree.Split(listOf(
                TraceTree.Repetition(
                    times = loopDescription.times,
                    subtree = TraceTree.Placeholder
                ),
                TraceTree.Placeholder
            ))
        ),
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
        guard = ::guardSuspend,
        application = ::ruleSuspend
    ),
    Rule(
        guard = ::guardRead,
        application = ::ruleRead
    ),
    Rule(
        guard = ::guardLoop,
        application = ::ruleLoop
    )
)


