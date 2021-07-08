package ai.evolution.state

import ai.evolution.utils.Utils.Companion.coinToss

/**
 * Condition definition for Genetic programming model.
 */
class Condition {

    var partialState: PartialState = PartialState()
    var abstractAction: AbstractAction = AbstractAction()

    /**
     * Mutates the condition. With probability of 0.5 partial state and abstractAction will be mutated.
     */
    fun mutate() {
        if (coinToss()) partialState.mutate()
        else abstractAction.mutate()
    }

    /**
     * Says how good the condition holds.
     */
    fun evaluate(realState: State): Double = realState.compareTo(partialState, abstractAction)

    fun getUnitAction(realState: State) = abstractAction.getUnitAction(realState)

    override fun toString() = "COND: \npartialState=$partialState,\n --> abstractAction=$abstractAction"

    companion object {
        const val PROB_MUT_USED = 0.15
    }
}