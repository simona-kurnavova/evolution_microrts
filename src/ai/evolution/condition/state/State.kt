package ai.evolution.condition.state

import ai.evolution.Utils.Companion.Entity
import ai.evolution.Utils.Companion.WIDTH
import ai.evolution.Utils.Companion.Keys
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.directionsWithoutNone
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.action.AbstractAction
import rts.GameState
import rts.PhysicalGameState
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitType
import kotlin.math.abs

open class State(val player: Int? = null, val gs: GameState? = null, val unit: Unit? = null) {

    val parameters = mutableMapOf<Keys, Boolean>()

    /**
     * Distance = 1; 4 directions
     */
    protected val entityClose: MutableList<Entity> = mutableListOf()

    /**
     * How much danger am I in: none=0, only enemies around me=1.
     * Range: [0,1]
     */
    protected var directDangerIndex: Double? = null

    /**
     * How dangerous is my surroundings; distance = WIDTH / 4.
     * Range: [0,1]
     */
    protected var dangerIndex: Double? = null

    protected var friendsAround: Double? = null

    protected var friendsEnemyRatio: Double? = null

    /**
     * Distance = WIDTH / 4
     */
    protected val entityAround: MutableList<Entity> = mutableListOf()

    /**
     * Closest enemy distance.
     */
    protected var enemyDistance: Double? = null

    /**
     * Closest resource distance.
     */
    protected var resourceDistance: Double? = null

    /**
     * Base distance.
     */
    protected var baseDistance: Double? = null

    /**
     * Number of resources hold by this unit.
     */
    protected var unitResources: Int? = null

    /**
     * Number of resources hold by player.
     */
    protected var playerResources: Int? = null

    var canProduce: Boolean? = null // enough resources and able to

    var canHarvest: Boolean? = null // not full and able to

    var canMove: Boolean? = null // is empty slot available and I am able to

    protected var strategy: Strategy = Strategy.NONE

    fun initialise() {

        val entitiesDistances = getEntitiesDistances(gs?.units)
        val maxDistance = gs?.physicalGameState?.width ?: WIDTH / 4

        val enemies = entitiesDistances.filter { isEnemy(it.key) }
        val resources = entitiesDistances.filter { isResource(it.key) }
        val friends = entitiesDistances.filter { isFriend(it.key) }
        val entitiesDistancesAround = entitiesDistances.filter { it.value <= maxDistance }

        // direct close positions information
        directionsWithoutNone.forEach { entityClose.add(getEntity(it, 1)) } // entities around
        directDangerIndex = entityClose.filter { it == Entity.ENEMY }.size.toDouble() / 4.0 // how dangerous entities around

        dangerIndex = entitiesDistancesAround.filter { isEnemy(it.key) }.size.toDouble() / ((2 * maxDistance) + 1) * ((2 * maxDistance) + 1)
        friendsAround = entitiesDistancesAround.filter { isFriend(it.key) }.size.toDouble() / ((2 * maxDistance) + 1) * ((2 * maxDistance) + 1)

        enemyDistance = enemies.values.first().toDouble() / WIDTH*WIDTH
        resourceDistance = resources.values.first().toDouble() / WIDTH*WIDTH

        val bases = entitiesDistances.filter { isMyBase(it.key) }.values
        baseDistance = if (!bases.isNullOrEmpty()) bases.first().toDouble() / WIDTH*WIDTH else null
        friendsEnemyRatio = friends.size.toDouble() / enemies.size.toDouble()

        // -------------------------------------------------------------------------------

        entitiesDistancesAround.forEach { entityAround.add(getEntityOnPosition(Pair(it.key.x, it.key.y))) }

        if (unit != null && player != null) {
            playerResources = gs?.getPlayer(player)?.resources ?: 0
            unitResources = if (isMyBase(unit)) playerResources else unit.resources

            canMove = unit.type.canMove && entityClose.contains(Entity.NONE)
            canHarvest = unit.type.canHarvest && entityClose.contains(Entity.RESOURCE)
            canProduce = entityClose.contains(Entity.NONE) && canAffordToProduce()
        }

        // -------------------------------------------------------------------------------

        parameters[Keys.ENEMY_CLOSE] = entityClose.contains(Entity.ENEMY) || entityClose.contains(Entity.ENEMY_BASE)
        parameters[Keys.RESOURCE_CLOSE] = entityClose.contains(Entity.RESOURCE)
        parameters[Keys.CARRY_RESOURCES] = unitResources != 0
        parameters[Keys.EMPTY_AROUND] = entityClose.filter { it != Entity.ENEMY }.size == 4
        parameters[Keys.AM_BASE] = unit?.type?.name == "Base"

        evaluateSituation()
    }

    /**
     * Determines global strategy.
     */
    private fun evaluateSituation() {
        if (directDangerIndex !! >= 0.5) // more than 2 enemies
            strategy = Strategy.ATTACK
        else if (dangerIndex !! >= 0.5 && friendsAround !! < 0.5)
            strategy = Strategy.DEFENSE
        else if (friendsEnemyRatio !! < 0.9)
            strategy = Strategy.HARVEST
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
        return getEntitiesDistances(entities).keys.first()
    }

    fun getEntitiesDistances(entities: List<Unit>?): Map<Unit, Int> {
        if (unit == null || entities.isNullOrEmpty()) return mapOf()
        return entities.map { it to getUnitDistance(it)}
                .sortedBy { (_, value) -> value }
                .toMap()
    }

    private fun getEntityOnPosition(position: Pair<Int, Int>): Entity {
        if (getTerrain(position) == PhysicalGameState.TERRAIN_NONE) {
            val unitOnPosition = gs?.units?.filter { it.x == position.first && it.y == position.second }

            if (unitOnPosition.isNullOrEmpty()) return Entity.NONE
            if (unitOnPosition[0] == unit) return Entity.ME

            with(unitOnPosition[0]) {
                if (player == -1) {
                    writeToFile("Detected resource")
                    return Entity.RESOURCE
                }
                if (isMyBase(this)) return Entity.MY_BASE
                if (isEnemyBase(this)) return Entity.ENEMY_BASE
                return if (isEnemy(this)) Entity.ENEMY
                else Entity.FRIEND
            }
        }
        return Entity.WALL
    }

    fun getUnitDistance(toUnit: Unit): Int {
        if (unit == null)
            return -1
        return abs(unit.x - toUnit.x) + abs(unit.y - toUnit.y)
    }

    fun getUnitDirection(toUnit: Unit, reverse: Boolean = false): List<Int> {
        val directions = mutableListOf<Int>()
        if (unit == null) return directions

        if (unit.y < toUnit.y) directions.add(DIRECTION_DOWN)
        else if (unit.y > toUnit.y) directions.add(DIRECTION_UP)

        if (unit.x < toUnit.x) directions.add(DIRECTION_RIGHT)
        else if (unit.x > toUnit.x) directions.add(DIRECTION_LEFT)

        if (reverse) {
            val reversedDirs = mutableListOf<Int>()
            directions.forEach { reversedDirs.add(opposite(it)) }
            return reversedDirs
        }
        return directions
    }

    fun opposite(direction: Int): Int = when (direction) {
        DIRECTION_LEFT -> DIRECTION_RIGHT
        DIRECTION_RIGHT -> DIRECTION_LEFT
        DIRECTION_UP -> DIRECTION_DOWN
        DIRECTION_DOWN -> DIRECTION_UP
        else -> DIRECTION_NONE
    }

    /**
     * Return entity on position relative to this unit.
     */
    private fun getEntity(direction: Int, distance: Int): Entity =
        getEntityOnPosition(calculateNextPosition(direction, distance))

    fun getEmptyDirection(): List<Int> {
        val emptyDirections = mutableListOf<Int>()
        directionsWithoutNone.forEach {
            if (getEntity(it, 1) == Entity.NONE)
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
            DIRECTION_RIGHT -> unit.x + distance
            DIRECTION_LEFT -> unit.x - distance
            else -> unit.x
        }

        val destinationY: Int = when (direction) {
            DIRECTION_DOWN -> unit.y + distance
            DIRECTION_UP -> unit.y - distance
            else -> unit.y
        }
        Pair(destinationX, destinationY)
    } else Pair(0, 0)

    fun compareTo(partialState: PartialState, abstractAction: AbstractAction): Double {
        var result = 0

        partialState.parameters.keys.forEach {
            if (parameters[it] == partialState.parameters[it])
                result++
        }

        // Detects invalid condition for this unit in regard to strategy
        when (abstractAction.getUnitAction(this).type) {
            TYPE_HARVEST -> {
                //if (canHarvest != true) return 0.0
                result += 1
            }
            TYPE_MOVE -> {
                if (canMove != true) return 0.0
            }
            TYPE_ATTACK_LOCATION ->  {
                if (unit?.type?.canAttack != true) return 0.0
            }
            TYPE_RETURN -> {
                if (baseDistance == null || unitResources == 0) return 0.0
                result += 1
            }
            TYPE_NONE -> return 0.0 // no point in empty action, means there is nothing to do here
        }

        return result.toDouble() / partialState.getPriority().toDouble()
    }

    fun whatToProduce(): UnitType? {
        if (unit == null) return null
        unit.type.produces.shuffled().forEach {
            if (unitResources!! >= it.cost)
                return it
        }
        return null
    }

    companion object {
        enum class Strategy {
            NONE, ATTACK, DEFENSE, HARVEST
        }
    }
}