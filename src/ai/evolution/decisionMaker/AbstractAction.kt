package ai.evolution.decisionMaker

import ai.evolution.Utils
import rts.UnitAction
import rts.units.Unit
import rts.units.UnitTypeTable

class AbstractAction {

    var action: Int = Utils.actions.random()
    var entity: Utils.Companion.Entity? = null
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
            UnitAction.TYPE_NONE -> UnitAction(UnitAction.TYPE_NONE)
            UnitAction.TYPE_PRODUCE -> {
                val directions = realState.getEmptyDirection()
                val produces = realState.unit?.type?.produces?.filter { it.name == unitToProduce }
                if (directions.isNullOrEmpty() || produces.isNullOrEmpty()) UnitAction(UnitAction.TYPE_NONE)
                else UnitAction(UnitAction.TYPE_PRODUCE, realState.getEmptyDirection().random(), produces[0])
            }
            else -> getEntityAction(realState, { unit -> realState.isEntity(unit, entity) }, type == Type.FROM_ENTITY)
        }
    }

    fun onActionChangeSetup() {
        type = Type.TO_ENTITY
        when (action) {
            UnitAction.TYPE_PRODUCE -> unitToProduce = if (TrainingUtils.ALLOW_WORKERS_ONLY) "Worker"
            else UnitTypeTable(TrainingUtils.UTT_VERSION).unitTypes.random().name
            UnitAction.TYPE_ATTACK_LOCATION -> entity = if (Utils.coinToss(TrainingUtils.PROB_BASE_ATTACK))
                Utils.Companion.Entity.ENEMY_BASE else Utils.Companion.Entity.ENEMY
            UnitAction.TYPE_MOVE -> {
                entity = if (entity != null) entity else Utils.entitiesWithoutMe.random()
                type = types.random()
            }
        }
        forceConsistency()
    }

    fun forceConsistency() {
        when (action) {
            UnitAction.TYPE_HARVEST -> entity = Utils.Companion.Entity.RESOURCE // Harvest nearest resource
            UnitAction.TYPE_RETURN -> entity = Utils.Companion.Entity.MY_BASE
        }
        if (action != UnitAction.TYPE_PRODUCE) unitToProduce = null
    }

    private fun getEntityAction(realState: State, unitFilter: (Unit) -> Boolean, reverseDirection: Boolean = false): UnitAction {
        val toUnit = realState.getClosestEntity(realState.gs?.units?.filter { unitFilter(it) })
        if (toUnit != null) {
            val directions = realState.getUnitDirection(toUnit, reverseDirection)
            val range = if (action == UnitAction.TYPE_ATTACK_LOCATION) realState.unit?.attackRange else 1
            if (realState.getUnitDistance(toUnit) == range && !reverseDirection) {
                if (action == UnitAction.TYPE_ATTACK_LOCATION) return UnitAction(action, toUnit.x, toUnit.y)
                return UnitAction(action, directions[0])
            } else {
                val emptyDirections = realState.getEmptyDirection()
                directions.filter { emptyDirections.contains(it) && it != UnitAction.DIRECTION_NONE }.forEach { return UnitAction(UnitAction.TYPE_MOVE, it) }
                emptyDirections.forEach { return UnitAction(UnitAction.TYPE_MOVE, it) }
            }
        }
        return UnitAction(UnitAction.TYPE_NONE)
    }

    fun mutate() {
        action = Utils.actions.random()
        onActionChangeSetup()
    }

    override fun toString(): String {
        return "action=${getAction()}, entity=$entity, type=$type, unitToProd=${unitToProduce})"
    }

    private fun getAction() = when(action) {
            UnitAction.TYPE_ATTACK_LOCATION -> "Attack"
            UnitAction.TYPE_PRODUCE -> "Produce"
            UnitAction.TYPE_RETURN -> "Return"
            UnitAction.TYPE_HARVEST -> "Harvest"
            UnitAction.TYPE_MOVE -> "Move"
            else -> "None"
    }

    companion object {
        enum class Type {
            TO_ENTITY, FROM_ENTITY
        }
        val types = listOf(Type.TO_ENTITY, Type.FROM_ENTITY)
    }
}