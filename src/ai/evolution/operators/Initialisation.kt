package ai.evolution.operators

import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.LOAD_FROM_FILE
import ai.evolution.utils.TrainingUtils.LOAD_POPULATION_FILE
import ai.evolution.utils.StrategyTrainingUtils
import ai.evolution.utils.TrainingUtils.POPULATION
import ai.evolution.runners.TestingRunner
import ai.evolution.gpstrategy.StrategyDecisionMaker

/**
 * Initialisation of population.
 */
object Initialisation {
    /**
     * Initialise random population of [POPULATION] size with [CONDITION_COUNT] number of conditions for each unit.
     */
    fun simpleInit(strategy: Boolean = false): MutableList<UnitDecisionMaker> = mutableListOf<UnitDecisionMaker>().apply {
        repeat(POPULATION) {
            if (LOAD_FROM_FILE) {
                add(TestingRunner.loadDecisionMakerFromFile(LOAD_POPULATION_FILE, it))
            }
            else
                add(UnitDecisionMaker(TrainingUtils.CONDITION_COUNT).apply {
                if (strategy) strategyDecisionMaker = StrategyDecisionMaker(StrategyTrainingUtils.CONDITION_COUNT)
            })
        }
    }

    fun simpleStrategyInit() = simpleInit(true)
}