package ai.evolution.state

import ai.evolution.utils.Utils.Companion.coinToss
import ai.evolution.utils.Utils.Companion.keys
import ai.evolution.utils.TrainingUtils.UTT_VERSION
import rts.units.UnitTypeTable

/**
 * State used in condition for comparison with real state of game (it is a subset of [State]).
 */
class PartialState : State() {

    var unitType: String? = null

    private fun getRandomUnit(): String? = if (coinToss(0.5)) null  else
            UnitTypeTable(UTT_VERSION).unitTypes.filter { it.name != "Resource" }.random().name

    /**
     * Initialises randomly, only one value to start small.
     */
    init {
        parameters[keys.random()] = listOf(true, false).random()
    }

    /**
     * Mutates partialState. In either way:
     * - Adds random key
     * - Removes random key
     * - Flips value of random key
     * - Changes unit randomly
     */
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
}