package ai.evolution.operators

import ai.evolution.utils.Utils.Companion.PlayerStatistics
import ai.evolution.utils.TrainingUtils
import rts.ActionStatistics
import rts.Game
import rts.PhysicalGameState
import kotlin.math.abs

/**
 * Object for different fitness functions.
 */
object Fitness {

    /**
     * Basic fitness implementation with hitpoints, damage and production values. Takes time and victory into account.
     */
    fun basicFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> {
        var points = playerStats.damageDone.toDouble() + playerStats.produced - playerStats.enemyDamage +
                    getHp(game, player) + (getBaseHp(game, player) * 10)

        if (epoch == null || epoch >= TrainingUtils.ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300000 / game.gs.time
            }

        return Pair(points, game.gs.winner() == player)
    }

    /**
     * Focuses only on attack and hitpoints.
     */
    fun aggressiveFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> {
        var points = playerStats.damageDone.toDouble() + (getBaseHp(game, player) * 10)

        if (epoch == null || epoch >= TrainingUtils.ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300000 / game.gs.time
            }

        return Pair(points, game.gs.winner() == player)
    }

    /**
     * Focuses on production.
     */
    fun productiveFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> {
        var points = playerStats.resHarvested + playerStats.produced + (getBaseHp(game, player) * 10)

        if (epoch == null || epoch >= TrainingUtils.ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300000 / game.gs.time
            }

        return Pair(points, game.gs.winner() == player)
    }

    /**
     * Fitness does not take time of the game into account.
     */
    fun timeIndependentFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> {
        var points = playerStats.damageDone.toDouble() + playerStats.produced - playerStats.enemyDamage +
                getHp(game, player) + (getBaseHp(game, player) * 10)
        if (epoch == null || epoch >= TrainingUtils.ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300
            }
        return Pair(points, game.gs.winner() == player)
    }

    private fun getHp(game: Game, player: Int): Double {
        val stats = getStats(game.gs.physicalGameState)
        return stats[player].hp.toDouble() - stats[abs(player - 1)].hp
    }

    private fun getBaseHp(game: Game, player: Int): Double {
        val stats = getStats(game.gs.physicalGameState)
        return stats[player].hpBase.toDouble() - stats[abs(player - 1)].hpBase
    }

    private fun getStats(physicalGameState: PhysicalGameState): List<PlayerStatistics> {
        val players = ArrayList<PlayerStatistics>()
        for (p in physicalGameState.players) {
            val playerStatistics = PlayerStatistics(
                    id = p.id,
                    units = physicalGameState.units.filter { it.player == p.id },
                    hp = physicalGameState.units.filter { it.player == p.id }.sumBy { it.hitPoints },
                    hpBase = physicalGameState.units.filter { it.player == p.id && it.type.name == "Base" }.sumBy { it.hitPoints }
            )
            players.add(playerStatistics)
        }
        return players
    }
}