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
        val tempConditions = decisionMaker.conditions
        tempConditions.addAll(conditions)

        child.conditions.addAll(tempConditions.shuffled().filter { it.usedCount > 0 }.take(conditions.size))

        if (child.conditions.size < conditions.size) {
            child.conditions.addAll(tempConditions.filter { it.usedCount <= 0 }.shuffled()
                    .take(conditions.size - child.conditions.size))
        }
        return child
    }

    fun getCountUsedConditions(): Double =
            conditions.filter { it.usedCount > 0 }.size.toDouble() / conditions.size.toDouble()

    fun setUnused() {
        conditions.forEach { it.usedCount = 0 }
    }
}