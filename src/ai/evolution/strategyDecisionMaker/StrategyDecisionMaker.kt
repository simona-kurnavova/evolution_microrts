package ai.evolution.strategyDecisionMaker

import ai.evolution.DecisionMaker
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils.COND_MUT_PROB
import com.google.gson.Gson

class StrategyDecisionMaker(conditionCount: Int = 0): DecisionMaker() {
    val conditions = mutableListOf<StrategyCondition>()

    init { repeat(conditionCount) { conditions.add(StrategyCondition()) } }

    fun decide(globalState: GlobalState): Pair<StrategyCondition, Double> =
            conditions.map { it to globalState.compareTo(it.partialGlobalState) }
                    .toList().shuffled().maxByOrNull { (_, value) -> value }!!

    override fun mutate() {
        conditions.forEach { if (coinToss(COND_MUT_PROB)) it.mutate() }
    }

    fun crossover(decisionMaker: StrategyDecisionMaker): StrategyDecisionMaker {
        val child = StrategyDecisionMaker()

        for (i in conditions.indices) {
            child.addCondition(listOf(conditions[i], decisionMaker.conditions[i]).random())
        }
        return child
    }

    private fun addCondition(condition: StrategyCondition) {
        val gson = Gson()
        val conditionJson = gson.toJson(condition)
        val conditionCopy = gson.fromJson(conditionJson, StrategyCondition::class.java)
        conditions.add(conditionCopy)
    }
}