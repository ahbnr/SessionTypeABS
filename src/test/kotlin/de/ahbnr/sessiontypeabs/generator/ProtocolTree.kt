package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method

data class MethodDescription(
    val invocationTimes: Int,
    val body: ProgramTree
)

data class GeneratorState(
    val usedFutures: Set<Future>,
    val traces: TraceTree,
    val methods: Map<Pair<Class, Method>, MethodDescription>,
    val protocol: ProtocolTree
) {

    val actors: Set<Class>
        get() = methods.keys.map { it.first }.toSet()
}

class Call(
    val caller: Class,
    val future: Future,
    val callee: Class,
    val method: Method
)

data class LoopDescription(
    val times: Int,
    val excluded: Set<Future>
)

data class BranchingDescription(
    val numBranches: Int,
    val branchToEncode: Int,
    val choosingActor: Class
)

data class SuspensionOpportunity(
    val suspendableActor: Class,
    val suspendableFuture: Future,
    val suspendableMethod: Method,
    val awaitableFuture: Future,
    val reactivationTracePoint: List<Int>
)

data class SeedData(
    val resolvedFutures: Set<Future>,
    val suspendedFutures: Set<Future>,
    val suspensionPoint: SuspensionOpportunity?,
    val calls: Set<Call>,
    val tracePosition: List<Int>,
    val methodPositions: Map<Pair<Class, Method>, List<Int>>,
    val inLoop: Boolean,
    val inBranching: Boolean,
    val numOfExecutions: Int,
    val encodeTrace: Boolean
) {
    val activeActors: Set<Class>
        get() = calls
            .filter { !resolvedFutures.contains(it.future) } // FIXME Are suspended actors active? If not, also remove suspended actors from this list
            .map { it.callee }
            .toSet()
}

class RecursionApplicator (
    private val root: ProtocolTree,
    private val position: List<Int>,
    val seedData: SeedData
){
    fun replaceSeed(replacement: ProtocolTree) =
        replace(root, position, replacement)
}

sealed class ProtocolTree {
    data class Leaf(
        val typeFragement: GlobalType
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) = emptySet<RecursionApplicator>()
    }

    data class Seed(
        val data: SeedData
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) = setOf(
            RecursionApplicator(
                root,
                position,
                data
            )
        )
    }

    data class Repetition(
        val subtree: ProtocolTree
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) =
            subtree.recursionPoints(root, position + 0)
    }

    data class Branching(
        val subtrees: List<ProtocolTree>,
        val choosingActor: Class
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) = subtrees.mapIndexed {
                index, it -> it.recursionPoints(root, position + index)
        }.flatten().toSet()
    }

    data class Split(
        val subtrees: List<ProtocolTree>
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) = subtrees.mapIndexed {
            index, it -> it.recursionPoints(root, position + index)
        }.flatten().toSet()
    }

    fun recursionPoints(): Set<RecursionApplicator> =
        recursionPoints(this, emptyList())

    protected abstract fun recursionPoints(root: ProtocolTree, position: List<Int>): Set<RecursionApplicator>
}

fun replace(protocol: ProtocolTree, locations: List<Int>, replacement: ProtocolTree): ProtocolTree =
    when (protocol) {
        is ProtocolTree.Split -> protocol.copy(
            subtrees = protocol.subtrees.replaced(
                locations.head, // FIXME throw exception, if list empty
                replace(protocol.subtrees[locations.head], locations.tail, replacement)
            )
        )
        is ProtocolTree.Repetition -> protocol.copy(
            subtree = replace(
                protocol.subtree,
                locations.tail, // FIXME throw exception, if list empty
                replacement
            )
        )
        is ProtocolTree.Branching -> protocol.copy(
            subtrees = protocol.subtrees.replaced(
                locations.head, // FIXME throw exception, if list empty
                replace(protocol.subtrees[locations.head], locations.tail, replacement)
            )
        )
        is ProtocolTree.Seed -> replacement // TODO throw exception, if locations are not empty
        else -> throw RuntimeException(
            "Trying to replace or traverse an element during replacement in a protocol tree during QuickCheck data generation, " +
            "but the element is not meant to be replaced or traversed. " +
            "This probably means an invalid RecursionApplicator had been generated and applied, which should never happen."
        )
    }

fun collapse(protocolTree: ProtocolTree): GlobalType? = // TODO Introduce skip type and remove nullability
    when (protocolTree) {
        is ProtocolTree.Leaf -> protocolTree.typeFragement
        is ProtocolTree.Split -> {
            val collapsedSubtrees = protocolTree
                .subtrees
                .mapNotNull(::collapse)

            if (collapsedSubtrees.isEmpty()) {
                null
            }

            else {
                collapsedSubtrees.reduce { acc, globalType -> GlobalType.Concatenation(acc, globalType) }
            }
        }
        is ProtocolTree.Repetition -> when (val subType = collapse(protocolTree.subtree)) {
            null -> null
            else -> GlobalType.Repetition(
                repeatedType = subType
            )
        }
        is ProtocolTree.Branching -> {
            val collapsedSubtrees = protocolTree
                .subtrees
                .map {
                    collapse(it) ?: GlobalType.Skip
                }

            if (collapsedSubtrees.isEmpty()) {
                null
            }

            else {
                GlobalType.Branching(
                    c = protocolTree.choosingActor,
                    branches = collapsedSubtrees
                )
            }
        }
        is ProtocolTree.Seed -> null
    }
