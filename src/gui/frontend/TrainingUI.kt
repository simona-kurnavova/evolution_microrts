package gui.frontend

import ai.RandomBiasedAI
import ai.evolution.GeneticAI
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.DecisionMaker
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.PhysicalGameState
import rts.units.Unit
import rts.units.UnitTypeTable
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.random.Random


class TrainingUI {

    data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val resourceCount: Int, val hp: Int,
                                var lost: Boolean = false)

    class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double)
    
    companion object {
        const val population = 10 // must be dividable by 2
        const val conditions = 250
        const val epochs = 300
        const val candidateCount = 2
        const val cores = 4 // number of processor cores for parallelization

        fun main(gameSettings: GameSettings) {

            // Initialise population
            val candidates: MutableList<DecisionMaker> = mutableListOf()
            for (i in 0 until population) {
                val decisionMaker = DecisionMaker()
                decisionMaker.generateConditions(conditions)
                candidates.add(decisionMaker)
            }

            for (epoch in 0 until epochs) {
                writeToFile("Epoch $epoch")

                // Evaluate fitness
                val fitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(candidates, gameSettings)
                writeToFile("--- BEST: ${fitnessList[0].fitness}")
                writeToFile("--- AVG: ${fitnessList.sumByDouble { it.fitness } / fitnessList.size }")

                // Crossover
                val children: MutableList<DecisionMaker> = mutableListOf()
                for (i in 0 until population) {
                    val parentCandidates = fitnessList.shuffled().take(2 * candidateCount)
                    val parents: List<EvaluatedCandidate> = parentCandidates.sortedByDescending { it.fitness }.take(2)
                    children.add(parents[0].decisionMaker.crossover(parents[1].decisionMaker))
                }

                // Evaluate children
                val childrenFitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(children, gameSettings)

                // Selection
                candidates.clear()
                fitnessList.take(population / 2).forEach {
                    candidates.add(it.decisionMaker)
                    it.decisionMaker.setUnused()
                }
                childrenFitnessList.take(population / 2).forEach {
                    candidates.add(it.decisionMaker)
                    it.decisionMaker.setUnused()
                }
            }
        }

        private fun evaluateFitness(candidates: MutableList<DecisionMaker>, gameSettings: GameSettings): MutableList<EvaluatedCandidate>  {
            val fitnessList: MutableList<EvaluatedCandidate> = mutableListOf()

            val candidateLists: List<List<DecisionMaker>> = candidates.chunked(cores)
            for (list in candidateLists) {
                var index = Random.nextInt()
                list.parallelStream().map {
                    val AIs = listOf(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                            RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)))

                    var fitness = 0.0
                    for (i in AIs.indices) {
                        val game = Game(gameSettings, AIs[i], GeneticAI(it))
                        val stats: ActionStatistics = game.start()
                        fitness += calculateFitness(game, stats, 1.0)
                    }
                    fitness /= AIs.size // average fitness

                    fitnessList.add(EvaluatedCandidate(it, fitness))
                }.collect(Collectors.toMap<Any, Any, Any>({ ++index }) { p: Any -> p })
            }

            fitnessList.sortByDescending { it.fitness }
            return fitnessList
        }

        private fun calculateFitness(game: Game, playerStats: ActionStatistics, countUsedConditions: Double): Double {
            val stats = getStats(game.gs.physicalGameState)
            val points: Double = (stats[1].hp.toDouble() - stats[0].hp.toDouble())
            var gamePoints = playerStats.damageDone + playerStats.produced +
                    playerStats.resHarvested + playerStats.resToBase
            if (game.gs.winner() == 1) gamePoints += 100000

            return countUsedConditions * (gamePoints.toDouble() + points)
        }


        private fun getStats(physicalGameState: PhysicalGameState): List<PlayerStatistics> {
            val players = ArrayList<PlayerStatistics>()
            for (p in physicalGameState.players) {
                val playerStatistics = PlayerStatistics(
                        id = p.id,
                        units = physicalGameState.units.filter { it.player == p.id },
                        resourceCount = physicalGameState.units.filter { it.player == p.id }.sumBy { it.resources },
                        hp = physicalGameState.units.filter { it.player == p.id }.sumBy { it.hitPoints }
                )
                if (playerStatistics.units.isNullOrEmpty() ||
                        (playerStatistics.units.size == 1 && playerStatistics.units.get(0)?.type?.name == "Base")) {
                    playerStatistics.lost = true
                }
                players.add(playerStatistics)
            }
            return players
        }
    }
}