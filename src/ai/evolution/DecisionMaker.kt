package ai.evolution

import ai.evolution.Utils.Companion.coinToss

class DecisionMaker {

    var conditions: MutableList<Condition> = mutableListOf()

    fun decide(unitState: UnitState): MutableList<Action> {
        val optionList = mutableListOf<Action>()
        for (cond in conditions) {
            if(cond.evaluate(unitState)) {
                optionList.add(cond.action)
            }
        }
        return optionList
    }

    fun initialise(count: Int) {
        for (i in 0..count) {
            conditions.add(Condition())
        }
    }

    fun mutate() {
        for (cond in conditions) {
            cond.mutate()
        }
    }
    fun crossover(decisionMaker: DecisionMaker): DecisionMaker {
        val child = DecisionMaker()
        for (i in 0 until conditions.size) {
            child.conditions.add(
                    if (coinToss()) conditions[i].crossover(decisionMaker.conditions.random())
                    else decisionMaker.conditions[i].crossover(conditions.random())
            )
        }
        return child
    }
}