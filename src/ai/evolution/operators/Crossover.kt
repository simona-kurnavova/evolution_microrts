package ai.evolution.operators

import ai.evolution.Utils
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils.CANDIDATE_COUNT
import ai.evolution.decisionMaker.TrainingUtils.PARENT_COUNT
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker

/**
 * Object for crossover implementations.
 */
object Crossover {
    /**
     * Uses tournament selection for crossover of [candidatesFitnessList]. Returns children of size [POPULATION].
     */
    fun tournament(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> {
        val children = mutableListOf<UnitDecisionMaker>()
        repeat(POPULATION) {
            val parentCandidates = candidatesFitnessList.shuffled()
                    .take(PARENT_COUNT * CANDIDATE_COUNT)
                    .chunked(PARENT_COUNT)
            val parents = mutableListOf<Utils.Companion.UnitCandidate>()
            parentCandidates.forEach { parents.add(it.maxByOrNull { it.fitness }!!) }
            // Add child and MUTATE it
            children.add(parents[0].unitDecisionMaker.crossover(parents[1].unitDecisionMaker).apply { mutate() })
        }
        return children
    }


    fun strategyTournament(candidatesFitnessList: MutableList<Utils.Companion.StrategyCandidate>): MutableList<StrategyDecisionMaker> {
        val children = mutableListOf<StrategyDecisionMaker>()
        repeat(POPULATION) {
            val parentCandidates = candidatesFitnessList.shuffled()
                    .take(PARENT_COUNT * CANDIDATE_COUNT)
                    .chunked(PARENT_COUNT)
            val parents = mutableListOf<Utils.Companion.StrategyCandidate>()
            parentCandidates.forEach { parents.add(it.maxByOrNull { it.fitness }!!) }
            // Add child and MUTATE it
            children.add(parents[0].strategyDecisionMaker.crossover(parents[1].strategyDecisionMaker).apply { mutate() })
        }
        return children
    }
}