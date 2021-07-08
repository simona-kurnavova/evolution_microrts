package ai.evolution.runners

import ai.evolution.neat.Environment
import ai.evolution.neat.Genome
import ai.evolution.neat.Pool
import ai.evolution.neat.Species
import ai.evolution.utils.BudgetUtils
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.Utils
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import java.io.File
import java.util.ArrayList
import kotlin.system.measureNanoTime

class NeatRunner(gameSettings: GameSettings) : Environment {

    private val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
        TrainingUtils.getFitness(g, a, p, null)
    }

    var bestGenome: Genome? = null
    var topGenome: Genome? = null
    var topGenomeWins = 0

    var budget = TrainingUtils.BUDGET_INITIAL

    /**
     * Evaluates fitness for the whole [population] by running game simulations.
     */
    override fun evaluateFitness(population: ArrayList<Genome>) {
        topGenome = null
        topGenomeWins = 0
        population.chunked(TrainingUtils.CORES_COUNT).forEach { chunk ->
            chunk.parallelStream().forEach {
                val fitnessEval = gameRunner.runGameForAIs(GameRunner.getEvaluateLambda(it), TrainingUtils.getActiveAIS(),
                        print = false, budget, runsPerAi = 1)
                it.fitness = fitnessEval.first.toFloat()
                it.points = fitnessEval.first.toFloat()

                if (topGenome == null || fitnessEval.first > topGenome!!.points) {
                    topGenome = it
                    topGenomeWins = fitnessEval.second

                    if (bestGenome == null || topGenome!!.points >= bestGenome!!.points) {
                        bestGenome = topGenome
                    }

                }
            }
        }
    }

    /**
     * Adapts budget.
     */
    fun adaptBudget(epoch: Int, topGenome: Genome) {
        if (TrainingUtils.ADAPTIVE_BUDGET) {
            budget = if (TrainingUtils.BUDGET_EPOCH_STEP > 0)
                BudgetUtils.adaptBudget(epoch, budget)
            else BudgetUtils.adaptBudgetByConditionNeat(budget, topGenome)
        }
    }

    companion object {
        private val neatBestFile = File("${Utils.ROOT_OUTPUT_FOLDER}/best_genome")
        private val tmpBestFile = File("${Utils.ROOT_OUTPUT_FOLDER}/tmp_best_genome")

        /**
         * Runs NEAT training algorithm [RUNS]-times with [EPOCH_COUNT] number of epochs,
         * [TrainingUtils.POPULATION] size of population.
         */
        fun train(gameSettings: GameSettings) {

            println(TrainingUtils.getActiveAIS() + " population = ${TrainingUtils.POPULATION}" + " units = ${TrainingUtils.HIDDEN_UNITS}")
            val fitnessSum = mutableMapOf<Int, Float>()

            repeat(TrainingUtils.RUNS) {
                val neatRunner = NeatRunner(gameSettings)
                val pool = Pool().apply { initializePool() }
                var epochTime = 0.0

                repeat(TrainingUtils.EPOCH_COUNT) repeat@{ epoch ->
                    epochTime += measureNanoTime {
                        pool.evaluateFitness(neatRunner)
                        Utils.writeEverywhere("$epoch BEST: " + "${neatRunner.topGenome?.points}, wins ${neatRunner.topGenomeWins}")

                        if (!fitnessSum.containsKey(epoch)) fitnessSum[epoch] = 0F
                        fitnessSum[epoch] = fitnessSum[epoch]!! + neatRunner.topGenome!!.points

                        pool.breedNewGeneration()

                        if (epoch % TrainingUtils.SAVE_POPULATION_INTERVAL == 0) {
                            Utils.writeToFile(Gson().toJson(neatRunner.topGenome).toString(), tmpBestFile)
                            savePopulation(pool.species)
                        }

                        if (neatRunner.topGenome != null && neatRunner.topGenomeWins >= TrainingUtils.getActiveAIS().size) {

                            if (neatRunner.topGenome!!.points >= TrainingUtils.TRESHOLD_FITNESS &&
                                    (TrainingUtils.BEST_AI_EPOCH <= epoch || neatRunner.budget >= (TrainingUtils.BUDGET_UPPER_LIMIT / 2))
                                    && epoch % TrainingUtils.SAVE_POPULATION_INTERVAL == 0) {
                            }

                            neatRunner.adaptBudget(epoch, neatRunner.topGenome!!)
                        }
                    }
                }

                Utils.writeEverywhere("Time per one epoch (avg): ${epochTime / TrainingUtils.EPOCH_COUNT}")
                Utils.writeToFile(Gson().toJson(neatRunner.topGenome).toString(), neatBestFile)
                Utils.writeToFile(Gson().toJson(neatRunner.bestGenome).toString(), neatBestFile)
                savePopulation(pool.species)
            }

            fitnessSum.forEach {
                Utils.writeToFile("${it.key} BEST ${it.value / TrainingUtils.RUNS}", Utils.averageBestFile)
            }
        }

        /**
         * Saves population to [popListFile].
         */
        private fun savePopulation(species: ArrayList<Species>) {
            Utils.popListFile.delete()
            species.forEach {
                it.genomes.forEach { genome ->
                    Utils.writeToFile(Gson().toJson(genome).toString(), Utils.popListFile)
                }
            }
        }
    }
}