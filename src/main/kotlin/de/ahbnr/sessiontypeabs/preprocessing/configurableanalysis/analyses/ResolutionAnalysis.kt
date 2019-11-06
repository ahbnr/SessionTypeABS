package de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.analyses

import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ConfigurableAnalysisException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.ScopeClosingException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.SelfContainednessException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.exceptions.TransferException
import de.ahbnr.sessiontypeabs.preprocessing.configurableanalysis.throwNotAtomic
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.GlobalType

data class ResolutionAnalysis(
    val resolvedFutures: Set<Future> = emptySet(),
    val introducedInCurrentTypeNesting: Set<Future> = emptySet()
): ConfigurableAnalysis<ResolutionAnalysis> {
    override fun transfer(t: GlobalType) =
        when (t) {
            is GlobalType.Initialization -> this.copy(
                    introducedInCurrentTypeNesting = introducedInCurrentTypeNesting + t.f
                )
            is GlobalType.Interaction -> this.copy(
                    introducedInCurrentTypeNesting = introducedInCurrentTypeNesting + t.f
                )
            is GlobalType.Resolution -> this.copy(
                    resolvedFutures = resolvedFutures + t.f
                )
            is GlobalType.Fetching ->
                if (t.f in resolvedFutures) {
                    this.copy()
                }

                else {
                    throw TransferException(
                        type = t,
                        message = """
                            Actor ${t.c.value} can not fetch from future ${t.f.value} since it has not yet been
                            resolved.
                        """.trimIndent()
                    )
                }
            is GlobalType.Release -> this.copy()
            is GlobalType.Skip -> this.copy()
            else -> throwNotAtomic(t)
        }

    override fun merge(rhs: ResolutionAnalysis, branchingContext: GlobalType.Branching) =
        this.copy(
            resolvedFutures = resolvedFutures intersect rhs.resolvedFutures,
            introducedInCurrentTypeNesting = introducedInCurrentTypeNesting intersect rhs.introducedInCurrentTypeNesting
        )

    override fun selfContained(preState: ResolutionAnalysis, context: GlobalType) {
        if (this.resolvedFutures != preState.resolvedFutures) {
            throw SelfContainednessException(
                type = context,
                message = """
                    A repeated type may not resolve a future which has been introduced outside the repetition.
                """.trimIndent()
            )
        }
    }

    override fun closeScopes(preState: ResolutionAnalysis, context: GlobalType): ResolutionAnalysis {
        val introducedInSubScope = this.introducedInCurrentTypeNesting - preState.introducedInCurrentTypeNesting

        val unresolvedFuture = introducedInSubScope.find {
            it !in resolvedFutures
        }

        return if (unresolvedFuture == null) {
            this.copy(
                resolvedFutures = this.resolvedFutures - introducedInSubScope,
                introducedInCurrentTypeNesting = preState.introducedInCurrentTypeNesting
            )
        }

        else {
            throw ScopeClosingException(
                type = context,
                message = """
                    All futures introduced in a nested type must also be resolved within that nested type.
                    However, future ${unresolvedFuture.value} has not been resolved.
                """.trimIndent()
            )
        }
    }
}