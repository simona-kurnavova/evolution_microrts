package ai.evolution.gpstrategy

import ai.evolution.utils.TrainingUtils.UTT_VERSION
import ai.evolution.utils.Utils.Companion.actions
import ai.evolution.utils.Utils.Companion.coinToss
import ai.evolution.state.AbstractAction
import rts.UnitAction.TYPE_PRODUCE
import rts.units.UnitTypeTable

/**
 * Defines strategy of all units (shared).
 */
class Strategy {

    /**
     * Ordered list of actions.
     */
    private val actionPriorityList = mutableListOf<Int>()

    /**
     * Names of the unit types to produce (ordered).
     */
    private val producePriorityList = mutableListOf<String>()

    /**
     * Initiate at random - shuffle both of the lists.
     */
    init {
        actionPriorityList.addAll(actions.shuffled())
        val utt = UnitTypeTable(UTT_VERSION)
        utt.unitTypes.filter { !it.producedBy.isNullOrEmpty() }.shuffled().forEach {
            producePriorityList.add(it.name)
        }
    }

    /**
     * Mutate Strategy. Either switch two elements of [actionPriorityList] or [producePriorityList].
     */
    fun mutate() {
        if (coinToss())
            switchRandomTwo(actionPriorityList)

        if (coinToss())
            switchRandomTwo(producePriorityList)
    }

    /**
     * Switches two elements of the [list].
     */
    private fun <T> switchRandomTwo(list: MutableList<T>) {
        val index1: Int = (0 until list.size).random()
        val index2: Int = (0 until list.size).random()

        if (index1 != index2) {
            val tmp = list[index1]
            list[index1] = list[index2]
            list[index2] = tmp
        }
    }

    /**
     * Returns the value of the action (how important it is to perform the action based on the ordered list).
     */
    fun evaluateAction(abstractAction: AbstractAction): Int {
        val index = actionPriorityList.indexOf(abstractAction.action) + 1
        if (abstractAction.action == TYPE_PRODUCE) {
            val produceIndex = producePriorityList.indexOf(abstractAction.unitToProduce) + 1
            return (index + ((actionPriorityList.size / producePriorityList.size) * produceIndex)) / 2 // average of two values
        }
        return index
    }
}