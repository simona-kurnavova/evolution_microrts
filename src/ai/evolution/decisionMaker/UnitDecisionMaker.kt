package ai.evolution.decisionMaker

import ai.evolution.DecisionMaker
import ai.evolution.utils.TrainingUtils.COND_MUT_PROB
import ai.evolution.utils.Utils.Companion.coinToss
import ai.evolution.strategyDecisionMaker.GlobalState
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import com.google.gson.Gson

class UnitDecisionMaker(conditionCount: Int = 0): DecisionMaker() {

    val conditions = mutableListOf<Condition>()

    var strategyDecisionMaker: StrategyDecisionMaker? = null

    init {
        repeat(conditionCount) { conditions.add(Condition()) }
    }

    override fun mutate() {
        conditions.forEach { if (coinToss(COND_MUT_PROB)) it.mutate() }
        strategyDecisionMaker?.mutate()
    }

    fun crossover(decisionMaker: UnitDecisionMaker): UnitDecisionMaker {
        val child = UnitDecisionMaker()

        for (i in conditions.indices) {
            child.addCondition(listOf(conditions[i], decisionMaker.conditions[i]).random())
        }

        // For strategy AI only, nulls otherwise
        child.strategyDecisionMaker =
                strategyDecisionMaker?.crossover(decisionMaker.strategyDecisionMaker!!)
        return child
    }

    fun decide(realState: State): List<Pair<Condition, Double>> = conditions.map {
        it to realState.compareTo(it.partialState, it.abstractAction)
    }.toList().shuffled().sortedByDescending { (_, value) -> value }

    fun decide(realState: State, globalState: GlobalState): List<Pair<Condition, Double>> =
        conditions.map {
            it to realState.compareTo(it.partialState, it.abstractAction,
            strategyDecisionMaker?.decide(globalState)?.first?.strategy)
        }.toList().shuffled().sortedByDescending { (_, value) -> value }

    /**
     * Used when adding a condiition from parent during crossover - creates deep copy.
     */
    private fun addCondition(cond: Condition) {
        val gson = Gson()
        val conditionJson = gson.toJson(cond)
        val conditionCopy = gson.fromJson(conditionJson, Condition::class.java)
        conditions.add(conditionCopy)
    }
}