package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.tracing.TraceFragment
import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import java.util.*

fun <E> List<E>.replaced(index: Int, replacement: E): List<E> {
    return take(index) + replacement + drop(index + 1)
}

sealed class TraceTree {
    data class Leaf(
        val actor: Class,
        val fragment: TraceFragment
    ) : TraceTree()

    data class Split(
        val subtrees: List<TraceTree>
    ) : TraceTree()

    data class Repetition(
        val times: Int,
        val subtree: TraceTree
    ) : TraceTree()

    //data class Branching(
    //    val subtrees: List<TraceTree>
    //) : TraceTree()

    object Placeholder: TraceTree()
}

fun replace(traceTree: TraceTree, locations: List<Int>, replacement: TraceTree): TraceTree =
    when (traceTree) {
        is TraceTree.Split -> traceTree.copy(
            subtrees = traceTree.subtrees.replaced(
                locations.head,
                replace(traceTree.subtrees[locations.head], locations.tail, replacement)
            )
        )

        is TraceTree.Repetition -> traceTree.copy(
            subtree = replace(traceTree.subtree, locations.tail, replacement)
        )

        //is TraceTree.Branching -> traceTree.copy(
        //    subtrees = traceTree.subtrees.replaced(
        //        locations.head,
        //        replace(traceTree.subtrees[locations.head], locations.tail, replacement)
        //    )
        //)

        is TraceTree.Leaf -> replacement // TODO throw exception, if locations are not empty
        is TraceTree.Placeholder -> replacement
    }

// TODO move to util library
fun <T> replicate(times: Int, element: T): List<T> =
    Collections.nCopies(times, element)

fun replicateWithFreshFutures(times: Int, trace: List<TraceFragment>): List<TraceFragment> =
    replicate(
        times,
        trace
    )
        .mapIndexed { idx, replicatedTrace ->
            substituteCreatedFutures(replicatedTrace, "l$idx")
        }
        .flatten()

fun substituteCreatedFutures(trace: List<TraceFragment>, substitutionPostfix: String): List<TraceFragment> {
    val remapped: MutableMap<Future, Future> = mutableMapOf()

    return trace.map {
        when (it) {
            is TraceFragment.Invocation -> {
                val substituteFuture = Future("${it.future.value}$substitutionPostfix")
                remapped[it.future] = substituteFuture

                it.copy(
                    future = substituteFuture
                )
            }

            is TraceFragment.Reactivation ->
                it.copy(
                    future = remapped[it.future] ?: it.future
                )
        }
    }
}

fun collapse(actor: Class, traceTree: TraceTree): List<TraceFragment> =
    when (traceTree) {
        is TraceTree.Leaf -> if (traceTree.actor == actor) {
                listOf(traceTree.fragment)
            }

            else {
                emptyList()
            }

        is TraceTree.Split ->
            traceTree.subtrees.flatMap { collapse(actor, it) }

        is TraceTree.Repetition ->
            replicateWithFreshFutures(
                traceTree.times,
                collapse(actor, traceTree.subtree)
            )

        is TraceTree.Placeholder -> emptyList()
    }
