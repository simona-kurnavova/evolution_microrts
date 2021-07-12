package ai.evolution.gpstrategy

import ai.evolution.utils.TrainingUtils.UTT_VERSION
import rts.GameState
import rts.units.UnitType
import rts.units.UnitTypeTable
import kotlin.math.abs

/**
 * Global state of the game. Same for all units in the given time. Used for GP with strategy.
 */
open class GlobalState(val player: Int? = null, val gs: GameState? = null) {

    /**
     * Variables of the globalState.
     */
    protected var ratios = mutableMapOf<String, Int>()

    /**
     * Using [gs], gather data from the game about a current globalState.
     */
    fun initialise() {
        var res = 0.0
        var resEnemy = 0.0

        getUnitTypes().forEach {
            val units = gs?.units?.filter { unit -> unit.type.name == it.name }
            val myUnits = units?.filter { it.player == player }
            val enemyUnits = units?.filter { it.player != player && it.player != -1 }

            val ratio: Double = (1 + (myUnits?.size ?: 0)).toDouble() / (1 + (enemyUnits?.size ?: 0)).toDouble()
            ratios[it.name] = getRatioValue(ratio)

            res += (myUnits?.sumBy { it.resources } ?: 0).toDouble()
            resEnemy += (enemyUnits?.sumBy { it.resources } ?: 0).toDouble()
        }

        val bases = gs?.units?.filter { unit -> unit.type.name == "Base"}
        val ratio: Double = (bases?.filter { it.player == player }?.sumBy { it.hitPoints } ?: 1).toDouble() /
                (bases?.filter { it.player != player }?.sumBy { it.hitPoints } ?: 1).toDouble()
        ratios[BASE_HP] = getRatioValue(ratio)

        if (player != null && gs != null) {
            ratios[PLAYER_RESOURCES] = getRatioValue((1 + (gs.getPlayer(player)?.resources ?: 0)).toDouble() /
                    (1 + (gs.getPlayer(abs(player - 1))?.resources ?: 0)).toDouble())
        }
        ratios[UNIT_RESOURCES] = getRatioValue(res / resEnemy)
    }

    /**
     * Compares [partialGlobalState] to a real globalState and returns number indicating how well the states match.
     */
    fun compareTo(partialGlobalState: PartialGlobalState): Double {
        var result = 0
        partialGlobalState.ratios.keys.forEach {
            if (ratios[it] == partialGlobalState.ratios[it])
                result++
        }
        return result.toDouble() / partialGlobalState.ratios.size
    }

    /**
     * Return unit types that is player able to produce
     */
    protected fun getUnitTypes(): List<UnitType> =
        UnitTypeTable(UTT_VERSION).unitTypes.filter { it.producedBy.isNotEmpty() }

    /**
     * Returns all variables for globalState (their keys).
     */
    protected fun getKeys(): List<String> = mutableListOf(BASE_HP, PLAYER_RESOURCES, UNIT_RESOURCES).apply {
        addAll(getUnitTypes().map { it.name })
    }

    /**
     * Transforms continuous value of ratio to discreet on from set {-1, 0, 1}
     * @return value -1, 0, 1
     */
    private fun getRatioValue(ratio: Double): Int {
        val adjustedRatio = ratio - 1
        return if (adjustedRatio + TOLERANCE > 0 && adjustedRatio - TOLERANCE < 0) 0
        else if(adjustedRatio > 0) return 1
        else return -1
    }

    companion object {
        const val TOLERANCE: Double = 0.05 // the sensitivity of transformation in [getRatioValue].
        val possibleValues = listOf(-1, 0, 1)

        const val BASE_HP = "BASE_HP"
        const val UNIT_RESOURCES = "UNIT_RESOURCES"
        const val PLAYER_RESOURCES = "PLAYER_RESOURCES"
    }
}