package ai.evolution.condition.action

import ai.evolution.TrainingUtils.ALLOW_WORKERS_ONLY
import ai.evolution.TrainingUtils.PROB_BASE_ATTACK
import ai.evolution.TrainingUtils.UTT_VERSION
import ai.evolution.Utils.Companion.Entity
import ai.evolution.Utils.Companion.actions
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.entitiesWithoutMe
import ai.evolution.condition.state.State
import rts.UnitAction
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitTypeTable

class AbstractAction {

    var action: Int = actions.random()
    var entity: Entity? = null
    var type: Type = Type.TO_ENTITY

    /**
     * Name of the unit to produce.
     */
    var unitToProduce: String? = null

    init {
        onActionChangeSetup()
    }

    /**
     * Creates instance of UnitAction according to relative state of a game and unit.
     * @param realState State of the game
     * @return UnitAction with correct arguments
     */
    fun getUnitAction(realState: State): UnitAction {
        return when(action) {
            TYPE_NONE -> UnitAction(TYPE_NONE)
            TYPE_PRODUCE -> {
                val directions = realState.getEmptyDirection()
                val produces = realState.unit?.type?.produces?.filter { it.name == unitToProduce }
                if (directions.isNullOrEmpty() || produces.isNullOrEmpty()) UnitAction(TYPE_NONE)
                else UnitAction(TYPE_PRODUCE, realState.getEmptyDirection().random(), produces[0])
            }
            else -> getEntityAction(realState, { unit -> realState.isEntity(unit, entity) }, type == Type.FROM_ENTITY)
        }
    }

    private fun onActionChangeSetup() {
        type = Type.TO_ENTITY
        when (action) {
            TYPE_PRODUCE -> unitToProduce = if (ALLOW_WORKERS_ONLY) "Worker"
            else UnitTypeTable(UTT_VERSION).unitTypes.random().name
            TYPE_HARVEST -> entity = Entity.RESOURCE // Harvest nearest resource
            TYPE_ATTACK_LOCATION -> entity = if (coinToss(PROB_BASE_ATTACK)) Entity.ENEMY_BASE else Entity.ENEMY
            TYPE_RETURN -> entity = Entity.MY_BASE
            TYPE_MOVE -> {
                entity = if (entity != null) entity else entitiesWithoutMe.random()
                type = types.random()
            }
        }

        if (action != TYPE_PRODUCE)
            unitToProduce = null
    }

    private fun getEntityAction(realState: State, unitFilter: (Unit) -> Boolean, reverseDirection: Boolean = false): UnitAction {
        val toUnit = realState.getClosestEntity(realState.gs?.units?.filter { unitFilter(it) })
        if (toUnit != null) {
            val directions = realState.getUnitDirection(toUnit, reverseDirection)
            val range = if (action == TYPE_ATTACK_LOCATION) realState.unit?.attackRange else 1
            if (realState.getUnitDistance(toUnit) == range && !reverseDirection) {
                if (action == TYPE_ATTACK_LOCATION) return UnitAction(action, toUnit.x, toUnit.y)
                return UnitAction(action, directions[0])
            } else {
                val emptyDirections = realState.getEmptyDirection()
                directions.filter { emptyDirections.contains(it) && it != DIRECTION_NONE }.forEach { return UnitAction(TYPE_MOVE, it) }
                emptyDirections.forEach { return UnitAction(TYPE_MOVE, it)  }
            }
        }
        return UnitAction(TYPE_NONE)
    }

    fun mutate() {
        action = actions.random()
        onActionChangeSetup()
    }

    override fun toString(): String {
        return "action=${getAction()}, entity=$entity, type=$type, unitToProd=${unitToProduce})"
    }

    private fun getAction() = when(action) {
            TYPE_ATTACK_LOCATION -> "Attack"
            TYPE_PRODUCE -> "Produce"
            TYPE_RETURN -> "Return"
            TYPE_HARVEST -> "Harvest"
            TYPE_MOVE -> "Move"
            else -> "None"
    }

    companion object {
        enum class Type {
            TO_ENTITY, FROM_ENTITY
        }
        val types = listOf(Type.TO_ENTITY, Type.FROM_ENTITY)
    }
}