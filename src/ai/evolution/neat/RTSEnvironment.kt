package ai.evolution.neat

import ai.evolution.Utils
import ai.evolution.Utils.Companion.writeEverywhere
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.decisionMaker.TrainingUtils.CORES_COUNT
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.decisionMaker.TrainingUtils.getActiveAIS
import ai.evolution.neat.NEAT_Config.HIDDEN_NODES
import ai.evolution.operators.Fitness
import ai.evolution.runners.GameRunner
import com.google.gson.Gson
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
        private const val EPOCHS = 2000
        private const val neatRoot = "output/NEAT_pop=${POPULATION}_hidden=${HIDDEN_NODES}epochs=$EPOCHS"
        private val neatEpochsFile = File("$neatRoot/epochs")
        private val neatBestFile = File("$neatRoot/best_genome")


        fun train(gameSettings: GameSettings) {
            File(neatRoot).mkdirs()
            val rtsEnvironment = RTSEnvironment(gameSettings)
            val pool = Pool().apply { initializePool() }
            var topGenome: Genome = Genome()

            repeat(EPOCHS) {
                pool.evaluateFitness(rtsEnvironment)
                topGenome = pool.topGenome
                writeEverywhere("$it BEST: ${topGenome.points}", neatEpochsFile)
                pool.breedNewGeneration()
            }
            writeToFile(Gson().toJson(topGenome).toString(), neatBestFile)
        }
    }
}