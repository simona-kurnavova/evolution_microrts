package ai.evolution.condition.state

import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.Keys
import ai.evolution.Utils.Companion.keys

/**
 * State used in condition for comparison with real state of game.
 */
class PartialState : State() {

    /**
     * How much conditions this partial state has. Helps with calculation of condition weight.
     */
    var priority: Int = 0

    init {
        parameters[keys.random()] = listOf(true, false).random()
        priority++
    }

    fun mutate() {
        if (coinToss(0.4) && parameters.keys.size > 1) {
            // Remove one key
            parameters.remove(parameters.keys.random())
            priority--
        } else if (coinToss(0.3)) {
            // Switch value for different
            val randomNonNull: Keys = parameters.keys.shuffled().random()
            if (parameters.containsKey(randomNonNull))
                parameters[randomNonNull] = parameters[randomNonNull]?.not() ?: listOf(true, false).random()
        } else {
            // Add value
            keys.shuffled().forEach {
                if (!parameters.containsKey(it)) {
                    parameters[it] = listOf(true, false).random()
                    priority++
                    return
                }
            }
        }
    }

    override fun toString(): String {
        var string = "priority=$priority, "
        keys.forEach {
            if (parameters.containsKey(it))
                string += "${it.name}=${parameters[it]}, "
        }
        return string
    }

    companion object {
        const val TOLERANCE = 0.1
    }
}