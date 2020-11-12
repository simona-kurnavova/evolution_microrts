package ai.evolution

import rts.GameState
import rts.PhysicalGameState.TERRAIN_NONE
import rts.PhysicalGameState.TERRAIN_WALL
import rts.UnitAction
import rts.units.Unit
import ai.evolution.Utils.Companion.Entity
import ai.evolution.Utils.Companion.WIDTH
import ai.evolution.Utils.Companion.directionsWithoutNone
import rts.UnitAction.DIRECTION_NONE

class UnitState(val player: Int, val gs: GameState, val unit: Unit) {

    /**
     * Return entity on position relative to this unit.
     */
    fun getEntity(direction: Direction, distance: Int): Entity {
        val resultPosition = calculateNextPosition(direction, distance)

        if (getTerrain(resultPosition) == TERRAIN_NONE) {
            val unitOnPosition = gs.units.filter { it.x == resultPosition.first && it.y == resultPosition.second }

            if (unitOnPosition.isNullOrEmpty())
                return Entity.NONE

            if (unitOnPosition[0] == unit)
                return Entity.ME // it's me

            return when (unitOnPosition[0].player) {
                player -> Entity.FRIEND
                -1 -> Entity.RESOURCE
                else -> Entity.ENEMY
            }
        }
        return Entity.WALL
    }

    fun getEntity(direction: Int, distance: Int): Entity = getEntity(when(direction) {
            UnitAction.DIRECTION_UP -> Direction.UP
            UnitAction.DIRECTION_RIGHT -> Direction.RIGHT
            UnitAction.DIRECTION_LEFT -> Direction.LEFT
            UnitAction.DIRECTION_DOWN -> Direction.DOWN
            else -> Direction.NONE
        }, distance)

    private fun getTerrain(position: Pair<Int, Int>): Int {
        if (gs.physicalGameState.height > position.second + 1 && gs.physicalGameState.width > position.first + 1
                && position.second - 1 > 0 && position.first - 1 > 0) {
            return gs.physicalGameState.getTerrain(position.first, position.second)
        }
        return TERRAIN_WALL
    }

    private fun calculateNextPosition(direction: Direction, distance: Int): Pair<Int, Int> {
        val destinationX: Int = when(direction) {
            Direction.RIGHT -> unit.x + distance
            Direction.LEFT -> unit.x - distance
            else -> unit.x
        }

        val destinationY: Int = when (direction) {
            Direction.DOWN -> unit.y + distance
            Direction.UP -> unit.y - distance
            else -> unit.y
        }
        return Pair(destinationX, destinationY)
    }

    private fun findEntityInDirection(entity: Entity, direction: Int): Boolean {
        for (i in 1..WIDTH) {
            if (getEntity(direction, i) == entity) return true
        }
        return false
    }

    private fun getEntityDirection(entity: Entity, distance: Int): Int {
        directionsWithoutNone.forEach {
            if (getEntity(it, distance) == entity) return it
        }
        return DIRECTION_NONE
    }

    companion object {

        enum class Direction {
            NONE, UP, RIGHT, LEFT, DOWN
        }
    }
}