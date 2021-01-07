package ai.evolution.condition

import ai.evolution.TrainingUtils.COND_MUT_PROB
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.condition.action.AbstractAction
import ai.evolution.condition.state.PartialState
import ai.evolution.condition.state.State

class DecisionMaker(conditionCount: Int = 0) {

    val conditions: MutableList<Condition> = mutableListOf()

    init {
        repeat(conditionCount) { conditions.add(Condition()) }
    }

    fun decide(realState: State): List<Pair<Condition, Double>> = conditions.map {
        it to realState.compareTo(it.partialState, it.abstractAction)
    }.toList().shuffled().sortedByDescending { (_, value) -> value }

    fun mutate() {
        conditions.forEach { if (coinToss(COND_MUT_PROB)) it.mutate() }
    }

    fun crossover(decisionMaker: DecisionMaker): DecisionMaker {
        val child = DecisionMaker()

        for (i in conditions.indices) {
            child.addCondition(if(coinToss()) when {
                conditions[i].usedCount > decisionMaker.conditions[i].usedCount -> (conditions[i])
                decisionMaker.conditions[i].usedCount > conditions[i].usedCount -> decisionMaker.conditions[i]
                else -> listOf(conditions[i], decisionMaker.conditions[i]).random()
            } else listOf(conditions[i], decisionMaker.conditions[i]).random())
            child.conditions[i].usedCount = 0
        }
        return child
    }

    fun getCountUsedConditions(): Double =
            conditions.filter { it.usedCount > 0 }.size.toDouble() / conditions.size.toDouble()

    fun setUnused() {
        conditions.forEach { it.usedCount = 0 }
    }

    override fun toString(): String {
        var string = ""
        conditions.forEach { string += it.toString() }
        return string
    }

    fun addCondition(cond: Condition) {
        val cond2 = Condition()
        cond2.usedCount = 0

        val abstractAction = AbstractAction()
        abstractAction.action = cond.abstractAction.action
        abstractAction.type = cond.abstractAction.type
        abstractAction.entity = cond.abstractAction.entity
        cond2.abstractAction = abstractAction

        val partialState = PartialState()
        partialState.parameters.clear()

        cond.partialState.parameters.forEach {
            partialState.parameters[it.key] = it.value
        }
        cond2.partialState = partialState
        cond2.partialState.priority = partialState.parameters.size
        conditions.add(cond2)
    }
}