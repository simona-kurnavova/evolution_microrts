package ai.evolution.strategyDecisionMaker

import ai.evolution.Utils.Companion.coinToss

class PartialGlobalState : GlobalState() {

    init {
        val unitType = getUnitTypes().random()
        unitRatio[unitType.name] = possibleValues.random()
    }

    fun mutate() {
        if (coinToss(0.4) && unitRatio.keys.size > 1) {
            // Remove one key
            unitRatio.remove(unitRatio.keys.random())
        } else if (coinToss(0.3) && unitRatio.keys.size >= 1) {
            // Switch value for different
            unitRatio[unitRatio.keys.random()] = possibleValues.random()
        } else {
            // Add value
            getUnitTypes().shuffled().forEach {
                if (!unitRatio.containsKey(it.name)) {
                    unitRatio[it.name] = possibleValues.random()
                    return
                }
            }
        }
    }
}