package ai.evolution.condition.action

import ai.evolution.Utils.Companion.Entity
import ai.evolution.Utils.Companion.PROB_BASE_ATTACK
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.actions
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.entitiesWithoutMe
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.state.State
import rts.UnitAction
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitType

class AbstractAction {

    private var action: Int = actions.random()
    private var entity: Entity? = null
    private var type: Type = Type.TO_ENTITY

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
            TYPE_HARVEST -> getEntityAction(realState, {unit -> realState.isResource(unit)})
            TYPE_RETURN -> getEntityAction(realState, { unit -> realState.isBase(unit) })
            TYPE_ATTACK_LOCATION, TYPE_MOVE -> getEntityAction(realState, { unit -> realState.isEnemy(unit) },
                    type == Type.FROM_ENTITY)

            TYPE_PRODUCE -> {
                if (getUnitToProduce(realState) != null)
                    UnitAction(TYPE_PRODUCE, realState.getEmptyDirection().random(), getUnitToProduce(realState))
                else UnitAction(action)
            }
            else -> UnitAction(action) // TYPE_RETURN, TYPE_NONE
        }
    }

    private fun onActionChangeSetup() {
        when (action) {
            TYPE_HARVEST -> entity = Entity.RESOURCE // Harvest nearest resource
            TYPE_ATTACK_LOCATION ->
                entity = if (coinToss(PROB_BASE_ATTACK)) Entity.ENEMY_BASE else Entity.ENEMY
            TYPE_MOVE -> {
                entity = if (entity != null) entity else entitiesWithoutMe.random()
                type = types.random()
            }
        }
    }

    private fun getEntityAction(realState: State, unitFilter: (Unit) -> Boolean, reverseDirection: Boolean = false): UnitAction {
        val toUnit = realState.getClosestEntity(realState.gs?.units?.filter { unitFilter(it) })
        if (toUnit != null) {
            val directions = realState.getUnitDirection(toUnit, reverseDirection)
            if (realState.getUnitDistance(toUnit) == 1 && !reverseDirection) {
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

    private fun getUnitToProduce(realState: State): UnitType? {
        if (realState.canProduce!!) {
            val toProd = realState.whatToProduce()

            if (toProd != null)
                return toProd
        }
        return null
    }

    override fun toString(): String {
        return "action=${getAction()}, entity=$entity, type=$type)"
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