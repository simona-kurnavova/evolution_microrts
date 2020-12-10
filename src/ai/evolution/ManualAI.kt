package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import rts.GameState
import rts.PlayerAction
import rts.UnitAction
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitType
import rts.units.UnitTypeTable
import java.util.ArrayList
import kotlin.math.abs
import kotlin.collections.filter as filter

class ManualAI(private val unitTypeTable: UnitTypeTable? = null) : AI() {

    private var gs: GameState? = null

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        this.gs = gs
        val playerAction = PlayerAction()

        if (gs == null || gs.physicalGameState == null) {
            return playerAction
        }

        // My available units
        val myUnits = gs.physicalGameState.units.filter {
            it.player == player && gs.getActionAssignment(it) == null
        }.sortedBy { it.id }
        val enemyUnits = gs.physicalGameState.units.filter { it.player != player && it.player != -1 }

        val harvesters = myUnits.size / 2
        for (i in myUnits.indices) {
            val unit = myUnits[i]
            val possibleUnitActions = unit.getUnitActions(gs)
            val action = if (i <= harvesters) decideAction(unit, Job.HARVEST)
            else decideAction(unit, Job.ATTACK)

            if (gs.canExecuteAnyAction(player) && gs.isUnitActionAllowed(unit, action)
                    && possibleUnitActions.contains(action)) {
                    playerAction.addUnitAction(unit, action)
                    //println(action)
                } else playerAction.addUnitAction(unit, possibleUnitActions.random())
        }
        return playerAction
    }

    enum class Job {
        HARVEST, NONE, ATTACK
    }

    private fun decideAction(unit: Unit, job: Job): UnitAction {
        val unitState = gs?.let { UnitState(unit.player, it, unit) }

        if (!unit.type.produces.isNullOrEmpty()) {
            // If I am base, I do not move, I just produce if enough resources
            val toProd = gs?.getPlayer(unit.player)?.resources?.let { getMaxToProd(unit.type.produces, it) }
            val action = UnitAction(TYPE_PRODUCE, getEmptyDirections(unitState).random(), toProd)

            println("What to prod: $toProd")
            if (toProd != null) {
                return action
            } else if(unit.type.name == "Base" )
                return UnitAction(TYPE_NONE)
        }

        if (unit.type.canAttack) {
            // If I am surrounded by enemy, shoot
            val enemyDirection = enemyInRange(unitState, unit)
            if (enemyDirection != DIRECTION_NONE) {
                return UnitAction(TYPE_ATTACK_LOCATION, enemyDirection)
            }
        }

        // If I carry resources and base exists, return
        if (unit.resources > 0) {
            if (!gs?.units?.filter { it.player == unit.player && it.type.name == "Base" }.isNullOrEmpty())
                return UnitAction(TYPE_RETURN)
        }

        if (unit.type.canHarvest && job == Job.HARVEST) {

                // Find resource
                val resources = gs?.units?.filter { it.resources > 0 && it.type.name == "Resource" }
                val resource = getClosest(resources, unit)
                if (resource != null) {
                    println("Found resource!")
                    val direction = getDirection(unitState, unit, resource.x, resource.y)
                    if (direction != DIRECTION_NONE) {
                        println("Moving towards")
                        return UnitAction(TYPE_MOVE, direction)
                    }
                }
        }

        if (unit.type.canAttack) {

            if (unit.type.canMove) {
                println("Attacking")
                val enemy: Unit? = getClosest(gs?.units?.filter {
                    it.player != -1 && it.player != unit.player
                }, unit)

                if (enemy != null) {
                    val direction = getDirection(unitState, unit, enemy.x, enemy.y)
                    if (direction != DIRECTION_NONE)
                        return UnitAction(TYPE_MOVE, direction)
                }
            }
            // if enemy base exists
            println("Find base")
            val bases = gs?.units?.filter { it.player != unit.player && it.type.name == "Base" }
            val base = if (!bases.isNullOrEmpty()) bases[0] else null
            if (base != null) {
                val direction = getDirection(unitState, unit, base.x, base.y)
                if (direction != DIRECTION_NONE)
                    return UnitAction(TYPE_MOVE, direction)
            }
        }
        return UnitAction(TYPE_NONE)
    }

    private fun getClosest(resources: List<Unit>?, unit: Unit): Unit? {
        if (resources.isNullOrEmpty())
            return null

        var closest: Unit? = null
        var closestDistance = 5 * (gs?.physicalGameState?.width ?: 100)

        for (res in resources) {
            val distance = abs(unit.x - res.x) + abs(unit.y - res.y)

            if (distance < closestDistance) {
                closestDistance = distance
                closest = res
            }
        }
        return closest
    }

    private fun getDirection(unitState: UnitState?, unit: Unit, x: Int, y: Int): Int {
        val emptyDirections = getEmptyDirections(unitState)
        val possibleDirections = mutableListOf<Int>()
        if (unit.x < x) {
            if (emptyDirections.contains(DIRECTION_RIGHT)) possibleDirections.add(DIRECTION_RIGHT)
            if (unit.y < y && emptyDirections.contains(DIRECTION_DOWN)) possibleDirections.add(DIRECTION_DOWN)
            if (unit.y > y && emptyDirections.contains(DIRECTION_UP)) possibleDirections.add(DIRECTION_UP)
        } else {
            if (emptyDirections.contains(DIRECTION_LEFT)) possibleDirections.add(DIRECTION_LEFT)
            if (unit.y < y && emptyDirections.contains(DIRECTION_DOWN)) possibleDirections.add(DIRECTION_DOWN)
            if (unit.y > y && emptyDirections.contains(DIRECTION_UP)) possibleDirections.add(DIRECTION_UP)
        }
        if (possibleDirections.isEmpty()) {
            return if (emptyDirections.size == DIRECTION_NONE) DIRECTION_NONE else emptyDirections.random()
        }
        return possibleDirections.random()
    }

    private fun enemyInRange(unitState: UnitState?, unit: Unit): Int =
            findEntityInRange(unitState, Utils.Companion.Entity.ENEMY, unit.attackRange)

    private fun findEntityInRange(unitState: UnitState?, entity: Utils.Companion.Entity, range: Int): Int {
        listOf(DIRECTION_UP, DIRECTION_RIGHT, DIRECTION_DOWN, DIRECTION_LEFT).shuffled().forEach {
            for (i in 1..range) {
                if (unitState?.getEntity(it, range) == entity) {
                    return it
                }
            }
        }
        return DIRECTION_NONE
    }

    private fun getMaxToProd(produces: ArrayList<UnitType>?, resources: Int): UnitType? {
        val possibilities = mutableListOf<UnitType>()
        println(resources)
        produces?.forEach {
            if (resources - it.cost >= 0) {
                possibilities.add(it)
                println(it)
            }
        }
        if (possibilities.isNullOrEmpty())
            return null
        return possibilities.sortedByDescending { it.hp }[0]
    }

    private fun getEmptyDirections(unitState: UnitState?): List<Int> {
        val dirs = mutableListOf<Int>()
        dirs.add(DIRECTION_NONE)
        listOf(DIRECTION_UP, DIRECTION_RIGHT, DIRECTION_DOWN, DIRECTION_LEFT).forEach {
            if (unitState?.getEntity(it, 1) == Utils.Companion.Entity.NONE)
                dirs.add(it)
        }
        return dirs
    }

    override fun clone(): AI = ManualAI(unitTypeTable)

    override fun getParameters(): MutableList<ParameterSpecification> = ArrayList()

    override fun reset() {}
}
