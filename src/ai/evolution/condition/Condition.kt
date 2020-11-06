package ai.evolution.condition

import ai.evolution.Utils.Companion.coinToss
import ai.evolution.condition.action.AbstractAction
import ai.evolution.condition.state.PartialState
import ai.evolution.condition.state.State

class Condition {

    var partialState: PartialState = PartialState()
    var abstractAction: AbstractAction = AbstractAction()

    fun mutate() {
        if (coinToss()) partialState.mutate()
        if (coinToss()) abstractAction.mutate()
    }

    fun evaluate(realState: State): Double = realState.compareTo(partialState)
}