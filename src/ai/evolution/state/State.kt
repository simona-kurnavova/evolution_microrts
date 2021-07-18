package ai.evolution.state

import ai.evolution.utils.Utils
import ai.evolution.utils.Utils.Companion.Entity
import ai.evolution.utils.Utils.Companion.Keys
import ai.evolution.utils.TrainingUtils.MAP_WIDTH
import ai.evolution.gpstrategy.Strategy
import ai.evolution.utils.TrainingUtils
import rts.GameState
import rts.PhysicalGameState
import rts.UnitAction
import rts.units.Unit
import rts.units.UnitType
import kotlin.math.abs

/**
 * State of the game. Represents the input to all the models.
 * Gathers data from [GameState] and convert them to simple bool values in [parameters].
 */
open class State(val player: Int? = null, val gs: GameState? = null, val unit: Unit? = null) {
    /**
     * Defines distance to "close objects".
     */
    private val tolerance = MAP_WIDTH / 4

    /**
     * Parameters representing state.
     */
    val parameters = mutableMapOf<Keys, Boolean>()

    /**
     * Distance = 1; 4 directions
     */
    val entityClose: MutableList<Entity> = mutableListOf()

    /**
     * Base distance.
     */
    protected var baseDistance: Double? = null

    /**
     * Number of resources hold by this unit.
     */
    private var unitResources: Int? = null

    /**
     * Number of resources hold by this player.
     */
    protected var playerResources: Int? = null

    var canHarvest: Boolean? = null // not full and able to
    var canMove: Boolean? = null // is empty slot available and I am able to
    var canAttack: Boolean? = null // is able to perform attack

    fun initialise() {

        val entitiesDistances = getEntitiesDistances(gs?.units)
        val enemies = entitiesDistances.filter { isEnemy(it.key) }
        val friends = entitiesDistances.filter { isFriend(it.key) }

        Utils.directionsWithoutNone.forEach { entityClose.add(getEntity(it, 1)) } // entities around
        val bases = entitiesDistances.filter { isMyBase(it.key) }.values
        val friendsEnemyRatio = friends.size.toDouble() / enemies.size.toDouble()

        baseDistance = if (!bases.isNullOrEmpty()) bases.first().toDouble() / TrainingUtils.MAP_WIDTH * TrainingUtils.MAP_WIDTH else null

        if (unit != null && player != null) {
            playerResources = gs?.getPlayer(player)?.resources ?: 0
            unitResources = if (isMyBase(unit)) playerResources else unit.resources

            canMove = unit.type.canMove && entityClose.contains(Entity.NONE)
            canHarvest = unit.type.canHarvest && entityClose.contains(Entity.RESOURCE)
            canAttack = unit.type.canAttack
        }

        // -------------------------------------------------------------------------------
        // Init of parameters:
        // -------------------------------------------------------------------------------

        // Resources
        parameters[Keys.HAVE_RESOURCES] = playerResources != 0
        parameters[Keys.CARRY_RESOURCES] = unitResources != 0
        parameters[Keys.RESOURCE_CLOSE] = entitiesDistances.filter { isResource(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.RESOURCE_REACHABLE] = entityClose.contains(Entity.RESOURCE)
        parameters[Keys.BASE_CLOSE] = entitiesDistances.filter { isMyBase(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.BASE_REACHABLE] = entityClose.contains(Entity.MY_BASE)

        // Produce
        parameters[Keys.EMPTY_AROUND] = entityClose.any { it == Entity.NONE }
        parameters[Keys.SAFE_AROUND] = entityClose.filter { it != Entity.ENEMY && it != Entity.ENEMY_BASE }.size == 4

        // Walls
        parameters[Keys.WALL_AROUND] = entityClose.any { it == Entity.WALL }
        parameters[Keys.IN_CORNER] = entityClose.count { it == Entity.WALL } > 1

        // Attack
        parameters[Keys.AM_STRONG] = unit != null && unit.hitPoints > 1
        parameters[Keys.ENEMY_REACHABLE] = entityClose.contains(Entity.ENEMY) || entityClose.contains(Entity.ENEMY_BASE)
        parameters[Keys.ENEMY_CLOSE] = entitiesDistances.filter { isEnemy(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.SURROUNDED] = entityClose.filter { it == Entity.ENEMY }.size > 1

        // Enemy base
        parameters[Keys.ENEMY_BASE_REACHABLE] = entityClose.any { it == Entity.ENEMY_BASE }
        parameters[Keys.ENEMY_BASE_CLOSE] = entitiesDistances.filter { isEnemyBase(it.key) && it.value < tolerance }
                .isNotEmpty()

        // Barracks
        parameters[Keys.HAVE_BARRACKS] = entitiesDistances.any { isMyBarracks(it.key) }
        parameters[Keys.ENEMY_HAVE_BARRACKS] = entitiesDistances.any { isEnemyBarracks(it.key) }
        parameters[Keys.ENEMY_BARRACKS_CLOSE] = entitiesDistances.filter { isEnemyBarracks(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.ENEMY_BARRACKS_REACHABLE] = entitiesDistances.filter { isEnemyBarracks(it.key) && it.value <= 1 }
                .isNotEmpty()

        parameters[Keys.BARRACKS_CLOSE] = entitiesDistances.filter { isMyBarracks(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.BARRACKS_REACHABLE] = entitiesDistances.filter { isMyBarracks(it.key) && it.value <= 1 }
                .isNotEmpty()

        // My team
        parameters[Keys.FRIEND_REACHABLE] = entityClose.any { it == Entity.FRIEND }
        parameters[Keys.FRIEND_CLOSE] = entitiesDistances.filter { isFriend(it.key) && it.value < tolerance }
                .isNotEmpty()
        parameters[Keys.OVERPOWERED] = friendsEnemyRatio < 0
    }

    /**
     * Compares [partialState] to real state (this). Assigns priority to the [abstractAction] based
     * on the match of the state, [strategy] (if available) and how good the action is in given state.
     */
    fun compareTo(partialState: PartialState, abstractAction: AbstractAction,
                  strategy: Strategy? = null): Double {
        var result = 0

        //if (unit?.type?.name == null || partialState.unitType == unit.type?.name)
        //    result++
        if (unit?.type?.name != null && partialState.unitType == unit.type?.name)
            result++


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

        val count = if (unit?.type?.name != null) partialState.parameters.size + 1 else partialState.parameters.size
        if (result == 0) return 0.0
        return result.toDouble() / count.toDouble()
    }

    fun isEnemy(unit: Unit) = unit.player != player && unit.player != -1

    fun isFriend(unit: Unit) = unit.player == player

    fun isResource(unit: Unit) = unit.player == -1

    fun isBase(unit: Unit) = unit.type.name == "Base"

    fun isBarracks(unit: Unit) = unit.type.name == "Barracks"

    fun isEnemyBarracks(unit: Unit) = isBarracks(unit) && isEnemy(unit)

    fun isMyBarracks(unit: Unit) = isBarracks(unit) && isFriend(unit)

    fun isMyBase(unit: Unit) = isBase(unit) && !isEnemy(unit)

    fun isEnemyBase(unit: Unit) = isBase(unit) && isEnemy(unit)

    fun getClosestEntity(entities: List<Unit>?): Unit? {
        val entityDistances = getEntitiesDistances(entities)
        if (entityDistances.keys.isNullOrEmpty()) return null
        return entityDistances.keys.first()
    }

    /**
     * Returns a list of [entities] with assigned distances relative to the current unit.
     */
    fun getEntitiesDistances(entities: List<Unit>?): Map<Unit, Int> {
        if (unit == null || entities.isNullOrEmpty()) return mapOf()
        return entities.map { it to getUnitDistance(it)}
                .filter { it.second >= 0 }
                .sortedBy { (_, value) -> value }
                .toMap()
    }

    /**
     * Returns entity on given [position].
     */
    private fun getEntityOnPosition(position: Pair<Int, Int>): Entity {
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

    /**
     * Calculates distance to the [toUnit].
     */
    fun getUnitDistance(toUnit: Unit): Int {
        if (unit == null)
            return -1
        return abs(unit.x - toUnit.x) + abs(unit.y - toUnit.y)
    }

    /**
     * Returns relative direction to the given [toUnit].
     * When [reverse] is true, the direction is opposite.
     */
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

    /**
     * Returns opposite direction.
     */
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
    private fun getEntity(direction: Int, distance: Int): Entity =
        getEntityOnPosition(calculateNextPosition(direction, distance))

    /**
     * Returns available empty direction (with no units or resources).
     */
    fun getEmptyDirection(): List<Int> {
        val emptyDirections = mutableListOf<Int>()
        Utils.directionsWithoutNone.forEach {
            if (getEntity(it, 1) == Utils.Companion.Entity.NONE)
                emptyDirections.add(it)
        }
        return emptyDirections
    }

    /**
     * Returns what kind of terrain is on given coordinates. Empty or wall.
     */
    private fun getTerrain(position: Pair<Int, Int>): Int {
        if (gs != null) {
            if (gs.physicalGameState.height > position.second + 1 && gs.physicalGameState.width > position.first + 1
                    && position.second - 1 >= 0 && position.first - 1 >= 0) {
                return gs.physicalGameState.getTerrain(position.first, position.second)
            }
            return PhysicalGameState.TERRAIN_WALL
        }
        return PhysicalGameState.TERRAIN_NONE
    }

    /**
     * Calculates next position of unit if going to [direction], in a given [distance].
     */
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

    /**
     * Returns [UnitType] that this unit is bale to produce and afford.
     */
    fun whatToProduce(): UnitType? {
        if (unit == null) return null
        unit.type.produces.shuffled().forEach {
            if (playerResources!! >= it.cost)
                return it
        }
        return null
    }

    /**
     * Returns true if [unit] is the type of entity [entity]. False otherwise.
     */
    fun isEntity(unit: Unit, entity: Entity?): Boolean {
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

    /**
     * Transform parameters into input array for NEAT.
     */
    fun getNeatInputs(): Array<Float> {
        val list = mutableListOf<Float>()
        parameters.forEach {
            list.add(it.value.toFloat())
        }
        gs?.unitTypeTable?.unitTypes?.forEach {
            list.add((unit?.type?.name == it.name).toFloat())
        }
        return list.toTypedArray()
    }

    private fun Boolean.toFloat() = if (this) 1.toFloat() else 0.toFloat()
}