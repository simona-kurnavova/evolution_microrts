package ai.evolution.neat

import ai.evolution.Utils.Companion.writeEverywhere
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.CORES_COUNT
import ai.evolution.decisionMaker.TrainingUtils.EPOCH_COUNT
import ai.evolution.decisionMaker.TrainingUtils.RUNS
import ai.evolution.decisionMaker.TrainingUtils.getActiveAIS
import ai.evolution.neat.NEAT_Config.HIDDEN_NODES
import ai.evolution.neat.NEAT_Config.POPULATION
import ai.evolution.operators.Fitness
import ai.evolution.runners.GameRunner
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import java.io.File
import java.util.ArrayList

class RTSEnvironment(gameSettings: GameSettings) : Environment {
    val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
        Fitness.basicFitness(g, a, p, null) }

    override fun evaluateFitness(population: ArrayList<Genome>) {
        population.chunked(CORES_COUNT).forEach { chunk ->
            chunk.parallelStream().forEach {
                val fitnessEval = gameRunner.runGameForAIs(gameRunner.getEvaluateLambda(it), getActiveAIS())
                it.fitness = fitnessEval.first.toFloat()
            }
        }
    }

    companion object {
        private const val neatRoot = "output/NEAT_pop=${POPULATION}_hidden=${HIDDEN_NODES}epochs=$EPOCH_COUNT"
        private val neatEpochsFile = File("$neatRoot/epochs")
        private val neatBestFile = File("$neatRoot/best_genome")
        private val neatAverageBestFile = File("$neatRoot/average_best")

        fun train(gameSettings: GameSettings) {
            File(neatRoot).mkdirs()
            val fitnessSum = mutableMapOf<Int, Float>()

            repeat(RUNS) { _ ->
                val rtsEnvironment = RTSEnvironment(gameSettings)
                val pool = Pool().apply { initializePool() }
                var topGenome = Genome()

                repeat(TrainingUtils.EPOCH_COUNT) { epoch ->
                    pool.evaluateFitness(rtsEnvironment)
                    topGenome = pool.topGenome
                    writeEverywhere("$epoch BEST: ${topGenome.points}", neatEpochsFile)
                    if (!fitnessSum.containsKey(epoch)) fitnessSum[epoch] = 0F
                    fitnessSum[epoch] = fitnessSum[epoch]!! + topGenome.points
                    pool.breedNewGeneration()
                }
                //writeToFile(Gson().toJson(topGenome).toString(), neatBestFile)
            }
            fitnessSum.forEach {
                writeToFile("${it.key} BEST ${it.value / RUNS}", neatAverageBestFile)
            }
        }
    }
}