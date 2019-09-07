package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Method

data class GeneratorState(
    val usedFutures: Set<Future>,
    val traces: TraceTree,
    val methods: Map<Pair<Class, Method>, ProgramTree>,
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
    val loops: List<LoopDescription>,
    val encodeProgram: Boolean
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

    data class Split(
        val subtrees: List<ProtocolTree>
    ) : ProtocolTree() {
        override fun recursionPoints(root: ProtocolTree, position: List<Int>) = subtrees.mapIndexed {
            index, it -> it.recursionPoints(root, position.plus(index))
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
                locations.head,
                replace(protocol.subtrees[locations.head], locations.tail, replacement)
            )
        )
        else -> replacement // TODO throw exception, if locations are not empty or protocol is not a leaf
    }

fun collapse(protocolTree: ProtocolTree): GlobalType? = // TODO Introduce skip type and remove nullability
    when (protocolTree) {
        is ProtocolTree.Leaf -> protocolTree.typeFragement
        is ProtocolTree.Split ->
            protocolTree
                .subtrees
                .mapNotNull(::collapse)
                .reduce { acc, globalType -> GlobalType.Concatenation(acc, globalType) }
        is ProtocolTree.Seed -> null
    }
