package gui.frontend

import ai.PassiveAI
import ai.RandomAI
import ai.RandomBiasedAI
import ai.evolution.GeneticProgrammingAI
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.DecisionMaker
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

    companion object {
        const val FITNESS_EVAL = 5
        val population = 15
        val conditions = 100
        val epochs = 100

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
                fitnessList.sortByDescending { it.fitness }

                writeToFile("--- BEST: ${fitnessList[0].fitness}")

                val max = fitnessList[fitnessList.size - 1].fitness
                val min = fitnessList[0].fitness

                writeToFile("--- AVG: ${fitnessList.sumByDouble { it.fitness } / fitnessList.size }")

                // Normalise
                fitnessList.forEach {
                    it.fitness = normalize(it.fitness, min, max)
                }

                // crossover
                val crossCandidates: MutableList<DecisionMaker> = mutableListOf()
                for (i in 0..5) {
                    crossCandidates.add(fitnessList.filter {
                        it.fitness > Random.nextDouble(0.0, 1.0)
                    }.random().decisionMaker)
                }

                val children: MutableList<DecisionMaker> = mutableListOf()
                for (i in 0..population) {
                    children.add(crossCandidates.random().crossover(crossCandidates.random()))
                }

                // Mutate children
                children.forEach { it.mutate() }

                // Eval children fitness
                writeToFile("Children:")
                val childrenFitnessList: MutableList<EvaluatedCandidate> = evaluateFitness(children, gameSettings)
                childrenFitnessList.sortByDescending { it.fitness }

                // Selection
                candidates.clear()
                childrenFitnessList.take(population / 2).forEach { candidates.add(it.decisionMaker) }
                fitnessList.take(population / 2).forEach { candidates.add(it.decisionMaker) }
                writeToFile("")
            }
        }

        fun normalize(value: Double, min: Double, max: Double): Double {
            return 1 - (value - min) / (max - min)
        }

        class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double)

        private fun evaluateFitness(candidates: MutableList<DecisionMaker>, gameSettings: GameSettings): MutableList<EvaluatedCandidate>  {
            val fitnessList: MutableList<EvaluatedCandidate> = mutableListOf()

            var index = 0
            candidates.parallelStream().map {
                val AIs = listOf(PassiveAI(UnitTypeTable(gameSettings.uttVersion)),
                        RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                        RandomAI(UnitTypeTable(gameSettings.uttVersion)))

                var fitness = 0.0
                for (i in AIs.indices) {
                    val game = Game(gameSettings, AIs[i], GeneticProgrammingAI(it))
                    game.start()
                    fitness += calculateFitness(game)
                }
                fitness /= AIs.size // average fitness

                fitnessList.add(EvaluatedCandidate(it, fitness))
                EvaluatedCandidate(it, fitness)
                writeToFile("Fitness = $fitness")
                index ++
            }.collect(Collectors.toMap<Any, Any, Any>({ index }) { p: Any -> p })
            return fitnessList
        }

        private fun calculateFitness(game: Game): Double {
            val stats = getStats(game.gs.physicalGameState)
            var points: Double = (stats[1].hp.toDouble() - stats[0].hp.toDouble())
            if (game.gs.winner() == 1) points += 500.0
            return points
            //return (points / game.gs.time) + 0.1
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