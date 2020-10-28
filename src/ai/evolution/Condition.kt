package ai.evolution

import ai.evolution.Utils.Companion.COND_MUT_PROB
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.writeToFile

class Condition {

    var direction: UnitState.Companion.Direction
    var distance: Int
    var entity: UnitState.Companion.Entity

    var action: Action = Action()

    val size = 16 // todo: global constant/param

    init {
        direction = directions.random()
        distance = (0..size).random()
        entity = entities.random()

        action.type = Action.possibleTypes.random()
        action.direction = Action.possibleDirections.random()
    }

    fun evaluate(state: UnitState): Boolean {
        val res = state.getEntity(direction, distance)
        writeToFile(" # Cond eval: $direction x $distance: real=$res | cond=$entity")
       return state.getEntity(direction, distance) == entity
    }

    fun mutate() {
        if (coinToss(COND_MUT_PROB)) {
            if (coinToss(0.25)) direction = directions.random()
            if (coinToss(0.25)) entity = entities.random()
            if (coinToss(0.25)) distance += (-(size/5)..(size/5)).random()
            if (coinToss(0.25)) action.mutate()
        }
    }

    fun crossover(condition: Condition): Condition {
        val child = Condition()
        child.direction = if (coinToss()) condition.direction else direction
        child.distance = if (coinToss()) condition.distance else distance
        child.entity = if (coinToss()) condition.entity else entity
        child.action.direction = if (coinToss()) condition.action.direction else action.direction
        child.action.type = if (coinToss()) condition.action.type else action.type

        return child
    }
    companion object {
        val directions = listOf(
                UnitState.Companion.Direction.NONE,
                UnitState.Companion.Direction.DOWN,
                UnitState.Companion.Direction.UP,
                UnitState.Companion.Direction.RIGHT,
                UnitState.Companion.Direction.LEFT)

        val entities = listOf(
                UnitState.Companion.Entity.NONE,
                UnitState.Companion.Entity.WALL,
                UnitState.Companion.Entity.ENEMY,
                UnitState.Companion.Entity.FRIEND,
                UnitState.Companion.Entity.RESOURCE
                //UnitState.Companion.Entity.ME
        )
    }
}