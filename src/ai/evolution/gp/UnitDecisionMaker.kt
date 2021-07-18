package ai.evolution.gp

import ai.evolution.state.Condition
import ai.evolution.state.State
import ai.evolution.gpstrategy.GlobalState
import ai.evolution.gpstrategy.StrategyDecisionMaker
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.Utils
import com.google.gson.Gson

/**
 * Decision maker for one unit.
 * Contains list of conditions based on which decides the best action for unit to take.
 */
class UnitDecisionMaker(conditionCount: Int = 0) {

    val conditions = mutableListOf<Condition>()

    var strategyDecisionMaker: StrategyDecisionMaker? = null // used only when training with strategy

    /**
     * Random init of the given number of conditions at the beginning of the training.
     */
    init {
        repeat(conditionCount) { conditions.add(Condition()) }
    }

    /**
     * Mutates list of conditions. Each condition has [COND_MUT_PROB] that it is going to be mutated.
     */
    fun mutate() {
        conditions.forEach { if (Utils.coinToss(TrainingUtils.COND_MUT_PROB)) it.mutate() }
        strategyDecisionMaker?.mutate()
    }

    /**
     * Creates new child from this and [decisionMaker].
     */
    fun crossover(decisionMaker: UnitDecisionMaker): UnitDecisionMaker {
        val child = UnitDecisionMaker()

        for (i in conditions.indices) {
            child.addCondition(listOf(conditions[i], decisionMaker.conditions[i]).random())
        }

        // For strategy AI only, null otherwise
        child.strategyDecisionMaker = strategyDecisionMaker?.crossover(decisionMaker.strategyDecisionMaker!!)
        return child
    }

    /**
     * Returns list of actions with assigned priorities. Sorted in descending order.
     */
    fun decide(realState: State): List<Pair<Condition, Double>> = conditions.map {
        it to realState.compareTo(it.partialState, it.abstractAction)
    }.toList().shuffled().filter { it.second > 0.001 }.sortedByDescending { (_, value) -> value }

    /**
     * Returns list of actions with assigned priorities. Sorted in descending order.
     * Used for Strategy GP, with added [globalState].
     */
    fun decide(realState: State, globalState: GlobalState): List<Pair<Condition, Double>> =
        conditions.map {
            it to realState.compareTo(it.partialState, it.abstractAction,
            strategyDecisionMaker?.decide(globalState)?.first?.strategy)
        }.toList().shuffled().sortedByDescending { (_, value) -> value }

    /**
     * Used when adding a condition from parent during crossover - creates deep copy.
     */
    private fun addCondition(cond: Condition) {
        val gson = Gson()
        val conditionJson = gson.toJson(cond)
        val conditionCopy = gson.fromJson(conditionJson, Condition::class.java)
        conditions.add(conditionCopy)
    }
}