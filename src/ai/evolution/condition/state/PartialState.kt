package ai.evolution.condition.state

import ai.evolution.Utils
import ai.evolution.Utils.Companion.PROB_STATE_GENERATE
import ai.evolution.Utils.Companion.PROB_STATE_MUTATE
import ai.evolution.Utils.Companion.coinToss
import ai.evolution.Utils.Companion.entitiesWithoutMe

class PartialState : State() {

    private var priority: Int = 0

    init {
        for (i in 1..4) {
            if (coinToss(PROB_STATE_GENERATE))
                entityClose.add(getRandomEntity())
            if (coinToss(PROB_STATE_GENERATE))
                entityAround.add(getRandomEntity())
        }
        enemyDistance = getSmallIntOrNull()
        resourceDistance = getSmallIntOrNull()
        baseDistance = getSmallIntOrNull()
        unitResources = getSmallIntOrNull()
        playerResources = getSmallIntOrNull()

        canProduce = getBoolOrNull()
        canHarvest = getBoolOrNull()
        canMove = getBoolOrNull()
    }

    fun getPriority() = priority

    private fun getSmallIntOrNull(): Int? {
        if (coinToss(PROB_STATE_GENERATE)) {
            priority ++
            return (0..4).random()
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
            if (coinToss(PROB_STATE_MUTATE))
                if (coinToss()) entityAround.add(getRandomEntity())
                else if (!entityAround.isNullOrEmpty()) {
                    entityAround.remove(entityAround.random())
                    priority --
                }
        }

        enemyDistance = getMutatedInt(enemyDistance)
        resourceDistance = getMutatedInt(resourceDistance)
        baseDistance = getMutatedInt(baseDistance)
        unitResources = getMutatedInt(unitResources)
        playerResources = getMutatedInt(playerResources)

        canProduce = getMutatedBool(canProduce)
        canHarvest = getMutatedBool(canHarvest)
        canMove = getMutatedBool(canMove)
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

    private fun getMutatedInt(variable: Int?): Int? {
        if (variable == null) return getSmallIntOrNull()
        if (coinToss(PROB_STATE_MUTATE)) {
            val value = (-TOLERANCE.. TOLERANCE).toList().random() + variable
            if (value < 0) {
                priority --
                return null
            }
            return value
        }
        return variable
    }

    companion object {
        const val TOLERANCE = 3
    }
}