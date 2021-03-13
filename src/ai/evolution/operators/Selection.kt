package ai.evolution.operators

import ai.evolution.DecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.Utils.Companion.StrategyCandidate
import ai.evolution.Utils.Companion.UnitCandidate
import ai.evolution.decisionMaker.UnitDecisionMaker


object Selection {
    fun selectBestPopulation(candidatesFitnessList: MutableList<UnitCandidate>,
                             childrenFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> =
            candidatesFitnessList.apply {
                addAll(childrenFitnessList)
                sortByDescending { it.fitness; it.wins }
            }.take(TrainingUtils.POPULATION).map { it.unitDecisionMaker }.toMutableList()

    fun selectBestStrategyPopulation(candidatesFitnessList: MutableList<StrategyCandidate>,
                                     childrenFitnessList: MutableList<StrategyCandidate>): MutableList<StrategyCandidate> =
            candidatesFitnessList.apply {
                addAll(childrenFitnessList)
                sortByDescending { it.fitness; it.wins }
            }.take(TrainingUtils.POPULATION).toMutableList()
}

