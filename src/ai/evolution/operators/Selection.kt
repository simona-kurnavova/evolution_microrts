package ai.evolution.operators

import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.Utils.Companion.UnitCandidate
import ai.evolution.gp.UnitDecisionMaker

/**
 * Implementation of selection.
 */
object Selection {
    /**
     * Selects [POPULATION] number of overall best individuals from previous and new generation.
     */
    fun selectBestPopulation(candidatesFitnessList: MutableList<UnitCandidate>,
                             childrenFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> =
            candidatesFitnessList.apply {
                addAll(childrenFitnessList)
                sortByDescending { it.fitness; it.wins }
            }.take(TrainingUtils.POPULATION).map { it.unitDecisionMaker }.toMutableList()
}

