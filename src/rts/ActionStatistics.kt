package rts

public class ActionStatistics {
    @JvmField
    var damageDone = 0
    @JvmField
    var resHarvested = 0
    @JvmField
    var resToBase = 0
    @JvmField
    var produced = 0
    @JvmField
    var moved = 0

    @JvmField
    var barracks = false

    var enemyProduced = 0
    var enemyDamage = 0
    var enemyHarvest = 0
    var enemyToBase = 0

    fun merge(stats: ActionStatistics) {
        damageDone += stats.damageDone
        resHarvested += stats.resHarvested
        resToBase += stats.resToBase
        produced += stats.produced
        moved += stats.moved
        barracks = stats.barracks

        enemyProduced += stats.enemyProduced
        enemyDamage += stats.enemyDamage
        enemyHarvest += stats.enemyHarvest
        enemyToBase += stats.enemyToBase
        //enemyStats.merge(stats.enemyStats)
    }

    fun mergeEnemy(stats: ActionStatistics) {
        enemyProduced += stats.produced
        enemyDamage += stats.damageDone
        enemyHarvest += stats.resHarvested
        enemyToBase += stats.resToBase
        //writeToFile("Enemy stats: $stats")
    }

    override fun toString(): String {
        return "ActionStats(damageDone=$damageDone, resHarvested=$resHarvested, resToBase=$resToBase, produced=$produced)"
    }
}