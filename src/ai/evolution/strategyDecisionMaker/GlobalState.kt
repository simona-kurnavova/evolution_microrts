package ai.evolution.strategyDecisionMaker

import ai.evolution.decisionMaker.TrainingUtils.UTT_VERSION
import rts.GameState
import rts.units.UnitType
import rts.units.UnitTypeTable

open class GlobalState(val player: Int? = null, val gs: GameState? = null) {
    protected var unitRatio = mutableMapOf<String, Int>()

    fun initialise() {
        getUnitTypes().forEach {
            val units = gs?.units?.filter { unit -> unit.type.name == it.name }

            val ratio: Double = if (units.isNullOrEmpty()) 0.0
            else units.filter { it.player == player }.size.toDouble() /
                    units.filter { it.player != player && it.player != -1 }.size

            unitRatio[it.name] = getValue(ratio)
        }
    }

    fun compareTo(partialGlobalState: PartialGlobalState): Double {
        var result = 0
        partialGlobalState.unitRatio.keys.forEach {
            if (unitRatio[it] == partialGlobalState.unitRatio[it])
                result++
        }
        return result.toDouble() / partialGlobalState.unitRatio.size
    }

    /**
     * @return unit types that is player able to produce
     */
    protected fun getUnitTypes(): List<UnitType> =
        UnitTypeTable(UTT_VERSION).unitTypes.filter { it.producedBy.isNotEmpty() }

    /**
     * @return value -1, 0, 1
     */
    private fun getValue(ratio: Double): Int {
        return if (ratio + TOLERANCE > 0 && ratio - TOLERANCE < 0) 0
        else if(ratio > 0) return 1
        else return -1
    }

    companion object {
        const val TOLERANCE: Double = 0.05
        val possibleValues = listOf(-1, 0, 1)
    }
}