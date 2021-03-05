package ai.evolution.operators

import ai.evolution.Utils
import ai.evolution.decisionMaker.TrainingUtils
import rts.ActionStatistics
import rts.Game
import rts.PhysicalGameState
import kotlin.math.abs

object Fitness {

    fun basicFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> {
        val stats: List<Utils.Companion.PlayerStatistics> = getStats(game.gs.physicalGameState)
        val hp: Double = stats[player].hp.toDouble() - stats[abs(player - 1)].hp
        val hpBase: Double = stats[player].hpBase.toDouble() - stats[abs(player - 1)].hpBase

        var points = if (epoch != null && epoch < TrainingUtils.ACTIVE_START) playerStats.produced.toDouble() + (if (playerStats.barracks) 5 else 0)
        else {
            ((playerStats.damageDone.toDouble() + playerStats.produced + 1)
                    - (playerStats.enemyDamage + 1)) + hp + (hpBase * 10)
        }

        if (epoch == null || epoch >= TrainingUtils.ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300000 / game.gs.time
            }

        return Pair(points, game.gs.winner() == player)
    }

    private fun getStats(physicalGameState: PhysicalGameState): List<Utils.Companion.PlayerStatistics> {
        val players = ArrayList<Utils.Companion.PlayerStatistics>()
        for (p in physicalGameState.players) {
            val playerStatistics = Utils.Companion.PlayerStatistics(
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