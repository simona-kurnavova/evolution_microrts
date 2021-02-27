package ai.evolution.decisionMaker

import ai.evolution.Utils.Companion.coinToss

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
        return "COND: \npartialState=$partialState, " +
                "\n --> abstractAction=$abstractAction, " +
                "\n --> usedCount=$usedCount\n"
    }

    companion object {
        const val PROB_MUT_USED = 0.15
        const val PROB_MUT_UNUSED = 0.5
    }
}