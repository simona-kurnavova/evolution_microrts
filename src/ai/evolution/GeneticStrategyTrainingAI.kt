package ai.evolution

import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.operators.Initialisation
import rts.GameSettings

/**
 * Class defining training of GP model with strategy.
 */
class GeneticStrategyTrainingAI(gameSettings: GameSettings) : GeneticTrainingAI(gameSettings) {

    init {
        println("GeneticStrategyTrainingAI")
    }

    override fun initialisePopulation(): MutableList<UnitDecisionMaker> =
            Initialisation.simpleStrategyInit()
}