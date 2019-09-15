package de.ahbnr.sessiontypeabs.generator

import de.ahbnr.sessiontypeabs.types.Class
import de.ahbnr.sessiontypeabs.types.Future
import de.ahbnr.sessiontypeabs.types.Method
import java.lang.IllegalArgumentException
import kotlin.math.abs
import kotlin.random.Random

data class RandomSourceConfig (
    val seed: Int,
    val stepProbability: Double,
    val maxSteps: Int,
    val methodReuseProbability: Double,
    val actorReuseProbability: Double,
    val maxLoopTimes: Int,
    val maxBranchSplits: Int
) {
    init {
        if (maxBranchSplits <= 0) {
            throw IllegalArgumentException("At least one branch must be allowed when branching, thus, maxBranchSplits must be > 0.")
        }
    }
}

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

    fun newBranchingDescription(seedData: SeedData): BranchingDescription? {
        val numBranches = abs(random.nextInt()) % config.maxBranchSplits + 1
        val choosingActor = selectActiveActor(seedData)

        return if (choosingActor == null) {
            null
        }

        else {
            BranchingDescription(
                numBranches = numBranches,
                branchToEncode = (0 until numBranches).random(random),
                choosingActor = choosingActor
            )
        }
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

    fun selectActiveActor(seedData: SeedData): Class? {
        return with (seedData.activeActors) {
            if (isEmpty()) {
                null
            }

            else {
                random(random)
            }
        }
    }

    fun selectInactiveActor(seedData: SeedData): Class {
        val inactiveUsedActors = usedActors - seedData.activeActors

        return if (inactiveUsedActors.isEmpty() || random.nextDouble() > config.actorReuseProbability) {
            val actor = newActor()
            usedActors.add(actor) // FIXME use global state actors property instead. => Single source of truth

            actor
        } else {
            inactiveUsedActors.random(random)
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