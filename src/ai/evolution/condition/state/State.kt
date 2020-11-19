package ai.evolution.condition.state

import ai.evolution.Utils.Companion.Entity
import ai.evolution.Utils.Companion.directionsWithoutNone
import ai.evolution.condition.state.PartialState.Companion.TOLERANCE
import rts.GameState
import rts.PhysicalGameState
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitType
import kotlin.math.abs

open class State(val player: Int? = null, val gs: GameState? = null, val unit: Unit? = null) {

    /**
     * Distance = 1, 4 directions
     */
    protected val entityClose: MutableList<Entity> = mutableListOf()

    /**
     * Distance = WIDTH / 4
     */
    protected val entityAround: MutableList<Entity> = mutableListOf()

    /**
     * Closest enemy distance.
     */
    protected var enemyDistance: Int? = null

    /**
     * Closest resource distance.
     */
    protected var resourceDistance: Int? = null

    /**
     * Base distance.
     */
    protected var baseDistance: Int? = null

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

    fun initialise() {
        directionsWithoutNone.forEach { entityClose.add(getEntity(it, 1)) }

        val entitiesDistances = getEntitiesDistances(gs?.units)
        val maxDistance = gs?.physicalGameState?.width ?: 16 / 4
        entitiesDistances.forEach {
            if (it.value <= maxDistance) entityAround.add(getEntityOnPosition(Pair(it.key.x, it.key.y)))
        }

        enemyDistance = entitiesDistances.filter { isEnemy(it.key) }.values.first()
        resourceDistance = entitiesDistances.filter { isResource(it.key) }.values.first()

        val bases = entitiesDistances.filter { isMyBase(it.key) }.values
        baseDistance = if (!bases.isNullOrEmpty()) bases.first() else null

        if (unit != null && player != null) {
            playerResources = gs?.getPlayer(player)?.resources ?: 0
            unitResources = if (isMyBase(unit)) playerResources else unit.resources

            canMove = unit.type.canMove && entityClose.contains(Entity.NONE)
            canHarvest = unit.type.canHarvest && entityClose.contains(Entity.RESOURCE)
            canProduce = entityClose.contains(Entity.NONE) && canAffordToProduce()
        }
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
                if (player == -1) return Entity.RESOURCE
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
        else directions.add(DIRECTION_UP)

        if (unit.x < toUnit.x) directions.add(DIRECTION_RIGHT)
        else directions.add(DIRECTION_LEFT)

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

    fun compareTo(partialState: PartialState): Double {
        var result = 0
        partialState.entityClose.forEach { if (entityClose.contains(it)) result ++ }
        partialState.entityAround.forEach { if (entityAround.contains(it)) result ++ }
        result += compareInts(enemyDistance, partialState.enemyDistance)
        result += compareInts(resourceDistance, partialState.resourceDistance)
        result += compareInts(baseDistance, partialState.baseDistance)
        //result += compareInts(unitResources, partialState.unitResources)
        //result += compareInts(playerResources, partialState.playerResources)
        //result += compareBool(canHarvest, partialState.canHarvest)
        //result += compareBool(canMove, partialState.canMove)
        //result += compareBool(canProduce, partialState.canProduce)
        return partialState.getPriority().toDouble() / result.toDouble()
    }

    private fun compareBool(variable: Boolean?, variableEstimate: Boolean?): Int {
        if (variable == variableEstimate) return 1
        return 0
    }

    private fun compareInts(variable: Int?, variableEstimate: Int?): Int {
        if (variable == null || variableEstimate == null) return 0
        if ((variableEstimate - TOLERANCE..variableEstimate + TOLERANCE).contains(variable))
            return 1
        return 0
    }

    fun whatToProduce(): UnitType? {
        if (unit == null) return null
        unit.type.produces.shuffled().forEach {
            if (unitResources!! >= it.cost)
                return it
        }
        return null
    }
}