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
import kotlin.random.Random


class TrainingUI {

    data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val resourceCount: Int, val hp: Int,
                                var lost: Boolean = false)

    class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double)


    companion object {
        const val FITNESS_EVAL = 5
        val population = 14
        val conditions = 450
        val epochs = 300

        fun main(gameSettings: GameSettings) {

            // Initialise population
            val candidates: MutableList<DecisionMaker> = mutableListOf()
            for (i in 1..population) {
                val decisionMaker = DecisionMaker()
                decisionMaker.generateConditions(conditions)
                candidates.add(decisionMaker)
            }

            for (epoch in 0..epochs) {
                writeToFile("Epoch $epoch")

                val fitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(candidates, gameSettings)
                writeToFile("--- BEST: ${fitnessList[0].fitness}")
                writeToFile("--- AVG: ${fitnessList.sumByDouble { it.fitness } / fitnessList.size }")

                // crossover
                val children: MutableList<DecisionMaker> = mutableListOf()
                for (i in 0..population) {
                    val crossCandidates = fitnessList.filter {
                        it.fitness > Random.nextDouble(fitnessList[fitnessList.size - 1].fitness, fitnessList[0].fitness)
                    }
                    children.add(crossCandidates.random().decisionMaker.crossover(
                            crossCandidates.random().decisionMaker)
                    )
                }

                // Eval children fitness
                val childrenFitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(children, gameSettings)

                // Selection
                candidates.clear()
                candidates.add(fitnessList[0].decisionMaker) // add best
                fitnessList.addAll(childrenFitnessList)

                for (i in 1 until population) { // popuplation - 1
                    val candidate1 = fitnessList.shuffled()[0]
                    val candidate2 = fitnessList.shuffled()[1]
                    candidates.add(
                            if (candidate1.fitness > candidate2.fitness) candidate1.decisionMaker
                            else candidate2.decisionMaker
                    )
                    fitnessList.removeAll(listOf(candidate1, candidate2))
                }
                candidates.forEach { it.mutate() }
            }
        }

        private fun normalize(value: Double, min: Double, max: Double): Double {
            return 1 - (value - min) / (max - min)
        }

        private fun evaluateFitness(candidates: MutableList<DecisionMaker>, gameSettings: GameSettings): MutableList<EvaluatedCandidate>  {
            val fitnessList: MutableList<EvaluatedCandidate> = mutableListOf()

            var index = 0
            candidates.parallelStream().map {
                val AIs = listOf(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                        RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion))
                        //ManualAI(),
                        //RandomAI(UnitTypeTable(gameSettings.uttVersion)),
                        //RandomAI(UnitTypeTable(gameSettings.uttVersion))
                        )

                var fitness = 0.0
                for (i in AIs.indices) {
                    val game = Game(gameSettings, AIs[i], GeneticAI(it))
                    val stats: ActionStatistics = game.start()
                    fitness += calculateFitness(game, stats)
                }
                fitness /= AIs.size // average fitness

                fitnessList.add(EvaluatedCandidate(it, fitness))
                EvaluatedCandidate(it, fitness)
                //writeToFile("Fitness = $fitness")
                index ++
            }.collect(Collectors.toMap<Any, Any, Any>({ index }) { p: Any -> p })

            fitnessList.sortByDescending { it.fitness }
            return fitnessList
        }

        private fun calculateFitness(game: Game, playerStats: ActionStatistics): Double {
            val stats = getStats(game.gs.physicalGameState)
            //var points: Double = (stats[1].hp.toDouble() - stats[0].hp.toDouble())
            var gamePoints = playerStats.damageDone + playerStats.produced + playerStats.resHarvested + playerStats.resToBase

            if (game.gs.winner() == 1) gamePoints += 500
            return gamePoints.toDouble()
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