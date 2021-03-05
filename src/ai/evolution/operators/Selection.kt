package ai.evolution.operators

import ai.evolution.Utils
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker

object Selection {
    fun selectBestPopulation(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>,
                             childrenFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> {
        val candidates = mutableListOf<UnitDecisionMaker>()
        candidatesFitnessList.addAll(childrenFitnessList)

        candidatesFitnessList.sortedByDescending { it.fitness }.take(TrainingUtils.POPULATION).forEach {
            candidates.add(it.unitDecisionMaker)
            it.unitDecisionMaker.setUnused()
        }
        return candidates
    }

    fun selectBestStrategyPopulation(candidatesFitnessList: MutableList<Utils.Companion.StrategyCandidate>,
                                     childrenFitnessList: MutableList<Utils.Companion.StrategyCandidate>): MutableList<StrategyDecisionMaker> {

        val candidates = mutableListOf<StrategyDecisionMaker>()
        candidatesFitnessList.addAll(childrenFitnessList)

        candidatesFitnessList.sortedByDescending { it.fitness }.take(TrainingUtils.POPULATION).forEach {
            candidates.add(it.strategyDecisionMaker)
        }
        return candidates
    }
}

