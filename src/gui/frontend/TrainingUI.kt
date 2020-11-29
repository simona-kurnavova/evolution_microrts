package gui.frontend

import ai.RandomAI
import ai.RandomBiasedAI
import ai.core.AI
import ai.evolution.GeneticAI
import ai.evolution.Utils.Companion.conditionsFile
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
        const val population = 24 // must be dividable by 2
        const val conditions = 200
        const val epochs = 400
        const val candidateCount = 2
        const val cores = 8 // number of processor cores for parallelization

        var globalBestCanditate: EvaluatedCandidate? = null

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
                val fitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(candidates, gameSettings, epoch >= 20)
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
                val childrenFitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(children, gameSettings, epoch >= 20)

                // Selection
                candidates.clear()
                fitnessList.take(population / 2).forEach {
                    candidates.add(it.decisionMaker)
                    it.decisionMaker.setUnused()
                }
                childrenFitnessList.take(population / 2).forEach {
                    candidates.add(it.decisionMaker)
                }
                if (epoch % 10 == 0) {
                    writeToFile("EPOCH $epoch ------------------------------------------", conditionsFile)
                    writeToFile("fitness=${globalBestCanditate?.fitness}", conditionsFile)
                    writeToFile(globalBestCanditate?.decisionMaker.toString(), conditionsFile)

                }
            }
        }

        private fun evaluateFitness(candidates: MutableList<DecisionMaker>, gameSettings: GameSettings, useBestAI: Boolean = false): MutableList<EvaluatedCandidate>  {
            val fitnessList: MutableList<EvaluatedCandidate> = mutableListOf()

            val candidateLists: List<List<DecisionMaker>> = candidates.chunked(cores)
            for (list in candidateLists) {
                var index = Random.nextInt()
                val AIs = mutableListOf<AI>(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                        RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)))

                val bestDecisionMaker = globalBestCanditate?.decisionMaker
                if (bestDecisionMaker != null && useBestAI) AIs.add(GeneticAI(bestDecisionMaker))

                list.parallelStream().map {

                    var fitness = 0.0
                    for (i in AIs.indices) {
                        val game = Game(gameSettings, AIs[i], GeneticAI(it))
                        val stats: ActionStatistics = game.start()
                        fitness += calculateFitness(game, stats, it.getCountUsedConditions())
                    }
                    fitness /= AIs.size // average fitness

                    fitnessList.add(EvaluatedCandidate(it, fitness))
                }.collect(Collectors.toMap<Any, Any, Any>({ ++index }) { p: Any -> p })
            }

            fitnessList.sortByDescending { it.fitness }
            if (globalBestCanditate == null || globalBestCanditate!!.fitness <= fitnessList[0].fitness) {
                globalBestCanditate = fitnessList[0]
            }
            return fitnessList
        }

        private fun calculateFitness(game: Game, playerStats: ActionStatistics, countUsedConditions: Double): Double {
            val stats = getStats(game.gs.physicalGameState)
            val points: Double = (stats[1].hp.toDouble() - stats[0].hp.toDouble())
            var gamePoints = playerStats.damageDone + playerStats.produced +
                    playerStats.resHarvested + playerStats.resToBase
            if (game.gs.winner() == 1) gamePoints += 100000

            return countUsedConditions * (gamePoints.toDouble() + points + 200)
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