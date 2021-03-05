package ai.evolution.decisionMaker

import ai.evolution.Utils
import ai.evolution.strategyDecisionMaker.Strategy
import ai.evolution.strategyDecisionMaker.StrategyCondition
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import rts.GameState
import rts.PhysicalGameState
import rts.UnitAction
import rts.units.Unit
import rts.units.UnitType
import kotlin.math.abs

open class State(val player: Int? = null, val gs: GameState? = null, val unit: Unit? = null) {

    val parameters = mutableMapOf<Utils.Companion.Keys, Boolean>()

    /**
     * Distance = 1; 4 directions
     */
    private val entityClose: MutableList<Utils.Companion.Entity> = mutableListOf()

    /**
     * Base distance.
     */
    protected var baseDistance: Double? = null

    /**
     * Number of resources hold by this unit.
     */
    protected var unitResources: Int? = null

    /**
     * Number of resources hold by this player.
     */
    protected var playerResources: Int? = null

    var canProduce: Boolean? = null // enough resources and able to
    var canHarvest: Boolean? = null // not full and able to
    var canMove: Boolean? = null // is empty slot available and I am able to

    fun initialise() {

        val entitiesDistances = getEntitiesDistances(gs?.units)
        val maxDistance = gs?.physicalGameState?.width ?: TrainingUtils.MAP_WIDTH / 4

        val enemies = entitiesDistances.filter { isEnemy(it.key) }
        val friends = entitiesDistances.filter { isFriend(it.key) }
        val entitiesDistancesAround = entitiesDistances.filter { it.value <= maxDistance }
        //val resources = entitiesDistances.filter { isResource(it.key) }

        Utils.directionsWithoutNone.forEach { entityClose.add(getEntity(it, 1)) } // entities around
        val bases = entitiesDistances.filter { isMyBase(it.key) }.values
        val friendsEnemyRatio = friends.size.toDouble() / enemies.size.toDouble()

        baseDistance = if (!bases.isNullOrEmpty()) bases.first().toDouble() / TrainingUtils.MAP_WIDTH * TrainingUtils.MAP_WIDTH else null

        //val directDangerIndex = entityClose.filter { it == Entity.ENEMY }.size.toDouble() / 4.0 // how dangerous entities around
        //val dangerIndex = entitiesDistancesAround.filter { isEnemy(it.key) }.size.toDouble() / ((2 * maxDistance) + 1) * ((2 * maxDistance) + 1)
        //val friendsAround = entitiesDistancesAround.filter { isFriend(it.key) }.size.toDouble() / ((2 * maxDistance) + 1) * ((2 * maxDistance) + 1)
        //val enemyDistance = enemies.values.first().toDouble() / WIDTH*WIDTH
        //val resourceDistance = resources.values.first().toDouble() / WIDTH*WIDTH

        //val entityAround: MutableList<Entity> = mutableListOf()
        //entitiesDistancesAround.forEach { entityAround.add(getEntityOnPosition(Pair(it.key.x, it.key.y))) }

        if (unit != null && player != null) {
            playerResources = gs?.getPlayer(player)?.resources ?: 0
            unitResources = if (isMyBase(unit)) playerResources else unit.resources

            canMove = unit.type.canMove && entityClose.contains(Utils.Companion.Entity.NONE)
            canHarvest = unit.type.canHarvest && entityClose.contains(Utils.Companion.Entity.RESOURCE)
            canProduce = entityClose.contains(Utils.Companion.Entity.NONE) && canAffordToProduce()
        }

        // -------------------------------------------------------------------------------

        parameters[Utils.Companion.Keys.ENEMY_CLOSE] = entityClose.contains(Utils.Companion.Entity.ENEMY) || entityClose.contains(Utils.Companion.Entity.ENEMY_BASE)
        parameters[Utils.Companion.Keys.RESOURCE_CLOSE] = entityClose.contains(Utils.Companion.Entity.RESOURCE)
        parameters[Utils.Companion.Keys.CARRY_RESOURCES] = unitResources != 0
        parameters[Utils.Companion.Keys.EMPTY_AROUND] = entityClose.filter { it != Utils.Companion.Entity.ENEMY }.size == 4
        parameters[Utils.Companion.Keys.SURROUNDED] = entityClose.filter { it == Utils.Companion.Entity.ENEMY }.size > 1
        parameters[Utils.Companion.Keys.ENEMY_BASE_CLOSE] = entityClose.any { it == Utils.Companion.Entity.ENEMY_BASE }
        parameters[Utils.Companion.Keys.FRIEND_CLOSE] = entityClose.any { it == Utils.Companion.Entity.FRIEND }
        parameters[Utils.Companion.Keys.OVERPOWERED] = friendsEnemyRatio < 0
    }

    private fun canAffordToProduce(): Boolean {
        if (unit == null || unitResources == null)
            return false

        unit.type.produces.forEach {
            if (unitResources!! >= it.cost)
                return true
        }
        return false
    }

    fun isEnemy(unit: Unit) = unit.player != player && unit.player != -1

    fun isFriend(unit: Unit) = unit.player == player

    fun isResource(unit: Unit) = unit.player == -1

    fun isBase(unit: Unit) = unit.type.name == "Base"

    fun isMyBase(unit: Unit) = isBase(unit) && !isEnemy(unit)

    fun isEnemyBase(unit: Unit) = isBase(unit) && isEnemy(unit)

    fun getClosestEntity(entities: List<Unit>?): Unit? {
        val entityDistances = getEntitiesDistances(entities)
        if (entityDistances.keys.isNullOrEmpty()) return null
        return entityDistances.keys.first()
    }

    fun getEntitiesDistances(entities: List<Unit>?): Map<Unit, Int> {
        if (unit == null || entities.isNullOrEmpty()) return mapOf()
        return entities.map { it to getUnitDistance(it)}
                .filter { it.second >= 0 }
                .sortedBy { (_, value) -> value }
                .toMap()
    }

    private fun getEntityOnPosition(position: Pair<Int, Int>): Utils.Companion.Entity {
        if (getTerrain(position) == PhysicalGameState.TERRAIN_NONE) {
            val unitOnPosition = gs?.units?.filter { it.x == position.first && it.y == position.second }

            if (unitOnPosition.isNullOrEmpty()) return Utils.Companion.Entity.NONE
            if (unitOnPosition[0] == unit) return Utils.Companion.Entity.ME

            with(unitOnPosition[0]) {
                if (player == -1) {
                    Utils.writeToFile("Detected resource")
                    return Utils.Companion.Entity.RESOURCE
                }
                if (isMyBase(this)) return Utils.Companion.Entity.MY_BASE
                if (isEnemyBase(this)) return Utils.Companion.Entity.ENEMY_BASE
                return if (isEnemy(this)) Utils.Companion.Entity.ENEMY
                else Utils.Companion.Entity.FRIEND
            }
        }
        return Utils.Companion.Entity.WALL
    }

    fun getUnitDistance(toUnit: Unit): Int {
        if (unit == null)
            return -1
        return abs(unit.x - toUnit.x) + abs(unit.y - toUnit.y)
    }

    fun getUnitDirection(toUnit: Unit, reverse: Boolean = false): List<Int> {
        val directions = mutableListOf<Int>()
        if (unit == null) return directions

        if (unit.y < toUnit.y) directions.add(UnitAction.DIRECTION_DOWN)
        else if (unit.y > toUnit.y) directions.add(UnitAction.DIRECTION_UP)

        if (unit.x < toUnit.x) directions.add(UnitAction.DIRECTION_RIGHT)
        else if (unit.x > toUnit.x) directions.add(UnitAction.DIRECTION_LEFT)

        if (reverse) {
            val reversedDirs = mutableListOf<Int>()
            directions.forEach { reversedDirs.add(opposite(it)) }
            return reversedDirs
        }
        return directions
    }

    fun opposite(direction: Int): Int = when (direction) {
        UnitAction.DIRECTION_LEFT -> UnitAction.DIRECTION_RIGHT
        UnitAction.DIRECTION_RIGHT -> UnitAction.DIRECTION_LEFT
        UnitAction.DIRECTION_UP -> UnitAction.DIRECTION_DOWN
        UnitAction.DIRECTION_DOWN -> UnitAction.DIRECTION_UP
        else -> UnitAction.DIRECTION_NONE
    }

    /**
     * Return entity on position relative to this unit.
     */
    private fun getEntity(direction: Int, distance: Int): Utils.Companion.Entity =
        getEntityOnPosition(calculateNextPosition(direction, distance))

    fun getEmptyDirection(): List<Int> {
        val emptyDirections = mutableListOf<Int>()
        Utils.directionsWithoutNone.forEach {
            if (getEntity(it, 1) == Utils.Companion.Entity.NONE)
                emptyDirections.add(it)
        }
        return emptyDirections
    }

    private fun getTerrain(position: Pair<Int, Int>): Int {
        if (gs != null) {
            if (gs.physicalGameState.height > position.second + 1 && gs.physicalGameState.width > position.first + 1
                    && position.second - 1 > 0 && position.first - 1 > 0) {
                return gs.physicalGameState.getTerrain(position.first, position.second)
            }
            return PhysicalGameState.TERRAIN_WALL
        }
        return PhysicalGameState.TERRAIN_NONE
    }

    private fun calculateNextPosition(direction: Int, distance: Int): Pair<Int, Int> = if (unit != null) {
        val destinationX: Int = when (direction) {
            UnitAction.DIRECTION_RIGHT -> unit.x + distance
            UnitAction.DIRECTION_LEFT -> unit.x - distance
            else -> unit.x
        }

        val destinationY: Int = when (direction) {
            UnitAction.DIRECTION_DOWN -> unit.y + distance
            UnitAction.DIRECTION_UP -> unit.y - distance
            else -> unit.y
        }
        Pair(destinationX, destinationY)
    } else Pair(0, 0)

    fun compareTo(partialState: PartialState, abstractAction: AbstractAction,
                  strategy: Strategy? = null): Double {

        var result = 0

        partialState.parameters.keys.forEach {
            if (parameters[it] == partialState.parameters[it])
                result++
        }

        // Detects invalid condition for this unit
        when (abstractAction.getUnitAction(this).type) {
            UnitAction.TYPE_HARVEST -> if (canHarvest != true) return 0.0
            UnitAction.TYPE_MOVE -> if (canMove != true) return 0.0
            UnitAction.TYPE_ATTACK_LOCATION -> if (unit?.type?.canAttack != true) return 0.0
            UnitAction.TYPE_RETURN -> if (baseDistance == null || unitResources == 0) return 0.0
            UnitAction.TYPE_NONE -> return 0.0
        }

        if (strategy != null) {
            return  (result.toDouble() / partialState.parameters.size) *
                    strategy.evaluateAction(abstractAction)
        }

        return result.toDouble() / partialState.parameters.size
    }

    fun whatToProduce(): UnitType? {
        if (unit == null) return null
        unit.type.produces.shuffled().forEach {
            if (playerResources!! >= it.cost)
                return it
        }
        return null
    }

    fun isEntity(unit: Unit, entity: Utils.Companion.Entity?): Boolean {
        if (entity == null) return false
        return when (entity) {
            Utils.Companion.Entity.FRIEND -> isFriend(unit)
            Utils.Companion.Entity.RESOURCE -> isResource(unit)
            Utils.Companion.Entity.ENEMY -> isEnemy(unit)
            Utils.Companion.Entity.ENEMY_BASE -> isEnemyBase(unit)
            Utils.Companion.Entity.MY_BASE -> isMyBase(unit)
            else -> false
        }
    }

    fun getInputs(): Array<Float> {
        val list = mutableListOf<Float>()
        parameters.forEach {
            list.add(it.value.toInt().toFloat())
        }
        return list.toTypedArray()
    }

    private fun Boolean.toInt() = if (this) 1 else 0
}