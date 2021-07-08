package ai.evolution.utils

import ai.evolution.utils.Utils.Companion.UnitCandidate
import ai.evolution.neat.Genome
import ai.evolution.neat.Species
import ai.evolution.utils.TrainingUtils.SCALE_BEST

/**
 * Utils for budget adaptation.
 */
object BudgetUtils {
    /**
     * Simple budget adaptation.
     */
    fun adaptBudget(epoch: Int, oldBudget: Int, postUpdate: (() -> Unit)? = null): Int {
        if (TrainingUtils.BUDGET_EPOCH_STEP <= 0 || epoch <= 0) return oldBudget

        var budget = oldBudget
        if (TrainingUtils.ACTIVE_START > 0 && TrainingUtils.ACTIVE_START == epoch) {
            budget = TrainingUtils.BUDGET_INITIAL
            Utils.writeEverywhere("//Current budget (active start): $budget")
            if (postUpdate != null) postUpdate()
        }
        else if (epoch % TrainingUtils.BUDGET_EPOCH_STEP == 0 && epoch > 0 && budget < 100) {
            budget += TrainingUtils.BUDGET_ADAPT_CONSTANT
            Utils.writeEverywhere("//Current budget: $budget")
            if (postUpdate != null) postUpdate()
        }
        return budget
    }

    /**
     * Auto-adaptation of budget for GP model.
     */
    fun adaptBudgetByConditionSga(oldBudget: Int, candidatesFitnessList: List<UnitCandidate>): Int {
        val third: Int = candidatesFitnessList.size / SCALE_BEST
        val topThirdAvgFitness = candidatesFitnessList.take(third).sumByDouble { it.fitness } / third
        val topThirdAvgWins = candidatesFitnessList.take(third).sumBy { it.wins } / third

        if (topThirdAvgFitness > TrainingUtils.TRESHOLD_FITNESS && topThirdAvgWins >= TrainingUtils.getActiveAIS().size) {
            Utils.writeEverywhere("// Adapted budget to ${oldBudget + TrainingUtils.BUDGET_ADAPT_CONSTANT}")
            return oldBudget + TrainingUtils.BUDGET_ADAPT_CONSTANT
        }
        return oldBudget
    }

    /**
     * Auto-adaptation of budget for NEAT model.
     */
    fun adaptBudgetByConditionNeat(oldBudget: Int, topGenome: Genome): Int {
        if (topGenome.points > TrainingUtils.TRESHOLD_FITNESS) {
            Utils.writeEverywhere("// Adapted budget to ${oldBudget + TrainingUtils.BUDGET_ADAPT_CONSTANT}")
            return oldBudget + TrainingUtils.BUDGET_ADAPT_CONSTANT
        }
        return oldBudget
    }
}