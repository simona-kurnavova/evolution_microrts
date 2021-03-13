package ai.evolution.strategyDecisionMaker

import ai.evolution.decisionMaker.TrainingUtils

object StrategyTrainingUtils {
    const val POPULATION: Int = TrainingUtils.POPULATION / 4
    const val CONDITION_COUNT = 25 // number of conditions for one unit
    const val COND_MUT_PROB = 0.18
}