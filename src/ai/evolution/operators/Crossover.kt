package ai.evolution.operators

import ai.evolution.utils.Utils
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.utils.TrainingUtils.POPULATION
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker

/**
 * Object for crossover implementations.
 */
object Crossover {

    private const val CANDIDATE_COUNT = 2

    /**
     * Uses tournament selection for crossover of [candidatesFitnessList]. Returns children of size [POPULATION].
     */
    fun tournament(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> {
        val children = mutableListOf<UnitDecisionMaker>()
        repeat(POPULATION) {
            val parentCandidates = candidatesFitnessList
                    .shuffled()
                    .take(2 * CANDIDATE_COUNT)
                    .chunked(2)
            val parents = mutableListOf<Utils.Companion.UnitCandidate>()
            parentCandidates.forEach { parents.add(it.maxByOrNull { it.fitness }!!) }

            // Add child and MUTATE it
            children.add(parents[0].unitDecisionMaker.crossover(parents[1].unitDecisionMaker)
                    .apply { mutate() })
        }
        return children
    }


    fun strategyTournament(candidatesFitnessList: MutableList<Utils.Companion.StrategyCandidate>): MutableList<StrategyDecisionMaker> {
        val children = mutableListOf<StrategyDecisionMaker>()
        repeat(POPULATION) {
            val parentCandidates = candidatesFitnessList.shuffled()
                    .take(2 * CANDIDATE_COUNT)
                    .chunked(2)
            val parents = mutableListOf<Utils.Companion.StrategyCandidate>()
            parentCandidates.forEach { parents.add(it.maxByOrNull { it.fitness }!!) }
            // Add child and MUTATE it
            children.add(parents[0].strategyDecisionMaker.crossover(parents[1].strategyDecisionMaker).apply { mutate() })
        }
        return children
    }
}