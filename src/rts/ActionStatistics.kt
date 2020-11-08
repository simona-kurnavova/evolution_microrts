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

    fun merge(stats: ActionStatistics) {
        damageDone += stats.damageDone
        resHarvested += stats.resHarvested
        resToBase += stats.resToBase
        produced += stats.produced
    }

    fun mergeEnemy(stats: ActionStatistics) {
        damageDone -= stats.damageDone
        resHarvested -= stats.resHarvested
        resToBase -= stats.resToBase
        produced -= stats.produced
    }

    override fun toString(): String {
        return "ActionStats(damageDone=$damageDone, resHarvested=$resHarvested, resToBase=$resToBase, produced=$produced)"
    }
}