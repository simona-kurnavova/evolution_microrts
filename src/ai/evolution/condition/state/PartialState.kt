package ai.evolution.condition.state

import ai.evolution.Utils
import ai.evolution.Utils.Companion.PROB_STATE_GENERATE
import ai.evolution.Utils.Companion.PROB_STATE_MUTATE
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.entitiesWithoutMe
import kotlin.random.Random

/**
 * State used in condition for comparison with real state of game.
 */
class PartialState : State() {

    /**
     * How much conditions this partial state has. Helps with calculation of condition weight.
     */
    private var priority: Int = 0

    init {
        if (coinToss(0.4))
            entityClose.add(getRandomEntity())

        directDangerIndex = getDoubleOrNull()
        dangerIndex = getDoubleOrNull()

        friendsAround = getDoubleOrNull()
        friendsEnemyRatio = getDoubleOrNull()

        enemyDistance = getDoubleOrNull()
        resourceDistance = getDoubleOrNull()
        baseDistance = getDoubleOrNull()
    }

    fun getPriority() = priority

    private fun getDoubleOrNull(): Double? {
        if (coinToss(PROB_STATE_GENERATE)) {
            priority ++
            return Random.nextDouble(0.0, 1.0)
        }
        return null
    }

    private fun getBoolOrNull(): Boolean? {
        if (coinToss(PROB_STATE_GENERATE)) {
            priority ++
            return listOf(true, false).random()
        }
        return null
    }

    private fun getRandomEntity(): Utils.Companion.Entity {
        priority++
        return entitiesWithoutMe.random()
    }

    fun mutate() {
        for (i in 1..4) {
            if (coinToss(PROB_STATE_MUTATE))
                if (coinToss()) entityClose.add(getRandomEntity())
                else if (!entityClose.isNullOrEmpty()) {
                    entityClose.remove(entityClose.random())
                    priority --
                }
        }

        directDangerIndex = getMutatedDouble(directDangerIndex)
        dangerIndex = getMutatedDouble(dangerIndex)

        friendsAround = getMutatedDouble(friendsAround)
        friendsEnemyRatio = getMutatedDouble(friendsEnemyRatio)

        enemyDistance = getMutatedDouble(enemyDistance)
        resourceDistance = getMutatedDouble(resourceDistance)
        baseDistance = getMutatedDouble(baseDistance)
    }

    private fun getMutatedBool(variable: Boolean?): Boolean? {
        var res = variable
        if (variable == null) return getBoolOrNull()
        if (coinToss(PROB_STATE_MUTATE)) {
            res = listOf(null, true, false).random()
            if (res == null) priority --
        }
        return res
    }

    private fun getMutatedDouble(variable: Double?): Double? {
        if (variable == null) return getDoubleOrNull()
        if (coinToss(PROB_STATE_MUTATE)) {
            val value =  Random.nextDouble(-MUT_TOLERANCE, MUT_TOLERANCE) + variable
            if (value < 0 || value > 1) {
                priority --
                return null
            }
            return value
        }
        return variable
    }

    override fun toString(): String {
        var string = "priority=$priority, entityClose=$entityClose"
        if (directDangerIndex != null) string += ", directDanger=$directDangerIndex"
        if (dangerIndex != null) string += ", danger=$dangerIndex"

        if (friendsAround != null) string += ", friendsAround=$friendsAround"
        if (friendsEnemyRatio != null) string += ", friends/Enemy=$friendsEnemyRatio"

        if (enemyDistance != null) string += ", enemyDist=$enemyDistance"
        if (resourceDistance != null) string += ", resourceDist=$resourceDistance"
        if (baseDistance != null) string += ", baseDist=$baseDistance"
        return string
    }

    companion object {
        const val MUT_TOLERANCE = 0.2
        const val TOLERANCE = 0.1
    }
}