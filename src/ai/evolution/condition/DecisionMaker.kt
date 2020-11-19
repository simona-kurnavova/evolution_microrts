package ai.evolution.condition

import ai.evolution.condition.state.State

class DecisionMaker {

    val conditions: MutableList<Condition> = mutableListOf()

    fun generateConditions(count: Int) {
        for (i in 0 until count) {
            conditions.add(Condition())
        }
    }

    fun decide(realState: State): List<Pair<Condition, Double>> = conditions.map {
        it to realState.compareTo(it.partialState)
    }.toList().sortedByDescending { (_, value) -> value }

    fun mutate() {
        conditions.forEach { it.mutate() }
    }

    fun crossover(decisionMaker: DecisionMaker): DecisionMaker {
        val child = DecisionMaker()

        for (i in conditions.indices) {
            child.conditions.add(when {
                conditions[i].usedCount > 0 -> (conditions[i])
                decisionMaker.conditions[i].usedCount > 0 -> decisionMaker.conditions[i]
                else -> listOf(conditions[i], decisionMaker.conditions[i]).random()
            })
            child.conditions[i].usedCount = 0
        }
        return child
    }

    fun getCountUsedConditions(): Double =
            conditions.filter { it.usedCount > 0 }.size.toDouble() / conditions.size.toDouble()

    fun setUnused() {
        conditions.forEach { it.usedCount = 0 }
    }
}