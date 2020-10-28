package ai.evolution

import rts.UnitAction
import rts.units.Unit


class Action {
    var type: Int = UnitAction.TYPE_NONE
    var direction: Int = UnitAction.DIRECTION_NONE

    fun getAction(unit: Unit): UnitAction {
        if (type == UnitAction.TYPE_PRODUCE) {
            if (unit.type.produces.isNullOrEmpty()) return UnitAction(UnitAction.TYPE_NONE)
            else return UnitAction(type, direction, unit.type.produces.random())
        }
        return UnitAction(type, direction)
    }

    fun mutate() {
        if (Utils.coinToss()) type = possibleTypes.filter { it != type }.random()
        else direction = possibleDirections.filter { it != direction }.random()
    }

    companion object {
        val possibleTypes = listOf(
                UnitAction.TYPE_NONE, UnitAction.TYPE_MOVE, UnitAction.TYPE_MOVE, UnitAction.TYPE_MOVE, UnitAction.TYPE_MOVE, UnitAction.TYPE_MOVE, UnitAction.TYPE_RETURN, UnitAction.TYPE_PRODUCE, UnitAction.TYPE_ATTACK_LOCATION, UnitAction.TYPE_HARVEST
        )
        val possibleDirections = listOf(
                UnitAction.DIRECTION_NONE, UnitAction.DIRECTION_DOWN, UnitAction.DIRECTION_UP, UnitAction.DIRECTION_RIGHT, UnitAction.DIRECTION_LEFT
        )
    }
}