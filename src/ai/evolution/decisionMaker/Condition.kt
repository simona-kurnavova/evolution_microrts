package ai.evolution.decisionMaker

import ai.evolution.utils.Utils.Companion.coinToss

class Condition {

    var partialState: PartialState = PartialState()
    var abstractAction: AbstractAction = AbstractAction()

    fun mutate() {
        if (coinToss()) partialState.mutate()
        else abstractAction.mutate()
    }

    fun getUnitAction(realState: State) = abstractAction.getUnitAction(realState)

    fun evaluate(realState: State): Double = realState.compareTo(partialState, abstractAction)

    override fun toString() = "COND: \npartialState=$partialState,\n --> abstractAction=$abstractAction"

    companion object {
        const val PROB_MUT_USED = 0.15
    }
}