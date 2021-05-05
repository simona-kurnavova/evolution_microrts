package ai.evolution.operators

import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.LOAD_FROM_FILE
import ai.evolution.decisionMaker.TrainingUtils.LOAD_POPULATION_FILE
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.runners.TestingRunner
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker

object Initialisation {
    /**
     * Initialise random population of [POPULATION] size with [CONDITION_COUNT] number of conditions for each unit.
     */
    fun simpleInit(strategy: Boolean = false): MutableList<UnitDecisionMaker> = mutableListOf<UnitDecisionMaker>().apply {
        repeat(POPULATION) {
            if (LOAD_FROM_FILE) {
                add(TestingRunner.loadAIFromFile(LOAD_POPULATION_FILE, it))
            }
            else
                add(UnitDecisionMaker(TrainingUtils.CONDITION_COUNT).apply {
                if (strategy) strategyDecisionMaker = StrategyDecisionMaker(StrategyTrainingUtils.CONDITION_COUNT)
            })
        }
    }

    fun simpleStrategyInit() = simpleInit(true)
}