package ai.evolution

import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils.CONDITION_COUNT
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils.POPULATION


class StrategyTrainingAI {
    var candidates = mutableListOf<StrategyDecisionMaker>()

    fun initialisePopulation() {
        repeat(POPULATION) {candidates.add(StrategyDecisionMaker(CONDITION_COUNT))}
    }

    fun mutate() {

    }

    fun crossover() {

    }
}