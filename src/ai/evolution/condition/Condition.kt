package ai.evolution.condition

import ai.evolution.Utils.Companion.coinToss
import ai.evolution.condition.action.AbstractAction
import ai.evolution.condition.state.PartialState
import ai.evolution.condition.state.State

class Condition {

    var partialState: PartialState = PartialState()
    var abstractAction: AbstractAction = AbstractAction()

    var usedCount = 0

    fun use() {
        usedCount++
    }

    fun mutate() {
        if (coinToss()) partialState.mutate()
        else abstractAction.mutate()
        usedCount = 0
    }

    fun getUnitAction(realState: State) = abstractAction.getUnitAction(realState)

    fun evaluate(realState: State): Double = realState.compareTo(partialState, abstractAction)
    override fun toString(): String {
        //if (usedCount == 0) return ""
        return "COND: \npartialState=$partialState, " +
                "\n --> abstractAction=$abstractAction, " +
                "\n --> usedCount=$usedCount\n"
    }

    companion object {
        const val PROB_MUT_USED = 0.15
        const val PROB_MUT_UNUSED = 0.5
    }
}