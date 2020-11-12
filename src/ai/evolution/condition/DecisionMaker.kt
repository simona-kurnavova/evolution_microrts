package ai.evolution.condition

import ai.evolution.condition.state.State
import rts.UnitAction

class DecisionMaker {

    val conditions: MutableList<Condition> = mutableListOf()

    fun generateConditions(count: Int) {
        for (i in 1..count) {
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
        decisionMaker.conditions.shuffled().take(conditions.size / 2).forEach { child.conditions.add(it) }
        conditions.shuffled().take(conditions.size / 2).forEach { child.conditions.add(it) }
        return child
    }
}