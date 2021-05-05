package ai.evolution.decisionMaker

import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.keys
import ai.evolution.decisionMaker.TrainingUtils.UTT_VERSION
import rts.units.UnitTypeTable

/**
 * State used in condition for comparison with real state of game.
 */
class PartialState : State() {

    var unitType: String? = getRandomUnit()

    private fun getRandomUnit(): String? = if (coinToss(0.3)) null else
            UnitTypeTable(UTT_VERSION).unitTypes.filter { it.name != "resource" }.random().name

    init {
        parameters[keys.random()] = listOf(true, false).random()
    }

    fun mutate() {
        if (coinToss(0.2))
            unitType = getRandomUnit()
        else if (coinToss(0.4) && parameters.keys.size > 1) {
            // Remove one key
            parameters.remove(parameters.keys.random())
        } else if (coinToss(0.3) && parameters.keys.size >= 1) {
            // Switch value for different
            val randomNonNull = parameters.keys.random()
            parameters[randomNonNull] = parameters[randomNonNull]?.not() ?: listOf(true, false).random()
        } else {
            // Add value
            keys.shuffled().forEach {
                if (!parameters.containsKey(it)) {
                    parameters[it] = listOf(true, false).random()
                    return
                }
            }
        }
    }

    companion object {
        const val TOLERANCE = 0.1
    }
}