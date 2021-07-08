package ai.evolution.gpstrategy

import ai.evolution.utils.Utils.Companion.coinToss

class PartialGlobalState : GlobalState() {

    init {
        val key = getKeys().random()
        ratios[key] = possibleValues.random()
    }

    fun mutate() {
        if (coinToss(0.4) && ratios.keys.size > 1) {
            // Remove one key
            ratios.remove(ratios.keys.random())
        } else if (coinToss(0.3) && ratios.keys.size >= 1) {
            // Switch value for different
            ratios[ratios.keys.random()] = possibleValues.random()
        } else {
            // Add value
            getUnitTypes().shuffled().forEach {
                if (!ratios.containsKey(it.name)) {
                    ratios[it.name] = possibleValues.random()
                    return
                }
            }
        }
    }
}