package ai.evolution

import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.operators.Initialisation
import rts.GameSettings

class GeneticStrategyTrainingAI(gameSettings: GameSettings) : GeneticTrainingAI(gameSettings) {

    override fun initialisePopulation(): MutableList<UnitDecisionMaker> =
            Initialisation.simpleStrategyInit()
}