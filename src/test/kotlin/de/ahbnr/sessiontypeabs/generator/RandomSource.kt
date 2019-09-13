package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import kotlin.math.abs
import kotlin.random.Random

data class RandomSourceConfig (
    val seed: Int,
    val stepProbability: Double,
    val maxSteps: Int,
    val methodReuseProbability: Double,
    val actorReuseProbability: Double,
    val maxLoopTimes: Int
)

class RandomSource (
    val config: RandomSourceConfig,
    var actorCounter: Int = 0,
    var futureCounter: Int = 0,
    var methodCounter: Int = 0,
    var stepCounter: Int = 0,
    val random: Random = Random(config.seed),
    private val usedActors: MutableList<Class> = mutableListOf()
) {
    fun newLoopDescription(): LoopDescription {
        return LoopDescription(
            times = abs(random.nextInt()) % config.maxLoopTimes,
            excluded = emptySet()
        )
    }

    fun newActor(): Class {
        return Class("A" + (actorCounter++))
    }

    fun newFuture(): Future {
        return Future("f" + (futureCounter++))
    }

    fun newMethod(): Method {
        return Method("m" + (methodCounter++))
    }

    fun <T> selectFromList(lst: List<T>) =
        if (lst.isEmpty()) {
            null
        }

        else {
            lst.random(random)
        }

    fun selectActor(seedData: SeedData): Class {
        val inactiveUsedActos = usedActors.minus(seedData.activeActors)

        return if (inactiveUsedActos.isEmpty() || random.nextDouble() > config.actorReuseProbability) {
            val actor = newActor()
            usedActors.add(actor) // FIXME use global state actors property instead. => Single source of truth

            actor
        } else {
            inactiveUsedActos.random(random)
        }
    }

    fun shouldRecurse(): Boolean {
        return stepCounter++ < config.maxSteps
            && random.nextDouble() <= config.stepProbability
    }

    fun selectRecursionApplicator(state: GeneratorState): RecursionApplicator? {
        val viableRecursionApplicators =
            state
                .protocol
                .recursionPoints()
                .filter { recursionApplicator -> // at least one rule must be applicable
                    rules.any {
                        it.guard(state, recursionApplicator.seedData)
                    }
                }

        return if (viableRecursionApplicators.isEmpty()) {
            null
        }

        else {
            viableRecursionApplicators.random(random)
        }
    }

    fun selectRule(state: GeneratorState, seedData: SeedData): Rule? {
        val viableRules = rules.filter { // Find those rules, whose guards allow their application in the given state
            it.guard(state, seedData)
        }

        return if (viableRules.isEmpty()) {
            null
        }

        else {
            viableRules.random(random)
        }
    }
}