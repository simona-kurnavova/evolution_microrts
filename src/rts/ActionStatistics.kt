package rts

internal class ActionStatistics {
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

    var enemyStats: Int = 0

    fun merge(stats: ActionStatistics) {
        damageDone += stats.damageDone
        resHarvested += stats.resHarvested
        resToBase += stats.resToBase
        produced += stats.produced
        moved += stats.moved
        enemyStats += stats.enemyStats
    }

    fun mergeEnemy(stats: ActionStatistics) {
        enemyStats += stats.damageDone + stats.resHarvested + stats.resToBase + stats.moved
        //writeToFile("Enemy stats: $stats")
    }

    override fun toString(): String {
        return "ActionStats(damageDone=$damageDone, resHarvested=$resHarvested, resToBase=$resToBase, produced=$produced)"
    }
}