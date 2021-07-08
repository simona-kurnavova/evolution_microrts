package ai.evolution.neat

import ai.evolution.utils.Utils.Companion.ROOT_OUTPUT_FOLDER
import ai.evolution.utils.Utils.Companion.writeEverywhere
import ai.evolution.utils.Utils.Companion.writeToFile
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.BUDGET_EPOCH_STEP
import ai.evolution.utils.TrainingUtils.BUDGET_INITIAL
import ai.evolution.utils.TrainingUtils.CORES_COUNT
import ai.evolution.utils.TrainingUtils.EPOCH_COUNT
import ai.evolution.utils.TrainingUtils.RUNS
import ai.evolution.utils.TrainingUtils.SAVE_POPULATION_INTERVAL
import ai.evolution.utils.TrainingUtils.getActiveAIS
import ai.evolution.runners.GameRunner
import ai.evolution.utils.BudgetUtils
import ai.evolution.utils.TrainingUtils.BEST_AI_EPOCH
import ai.evolution.utils.TrainingUtils.BUDGET_UPPER_LIMIT
import ai.evolution.utils.TrainingUtils.TRESHOLD_FITNESS
import ai.evolution.utils.Utils.Companion.averageBestFile
import ai.evolution.utils.Utils.Companion.popListFile
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import java.io.File
import java.util.ArrayList
import kotlin.system.measureNanoTime

class NeatRunner(gameSettings: GameSettings) : Environment {
    private val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
        TrainingUtils.getFitness(g, a, p, null) }

    var bestGenome: Genome? = null

    var topGenome: Genome? = null

    var topGenomeWins = 0

    var budget = BUDGET_INITIAL

    override fun evaluateFitness(population: ArrayList<Genome>) {
        topGenome = null
        topGenomeWins = 0
        population.chunked(CORES_COUNT).forEach { chunk ->
            chunk.parallelStream().forEach {
                val fitnessEval = gameRunner.runGameForAIs(GameRunner.getEvaluateLambda(it), getActiveAIS(),
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

    fun adaptBudget(epoch: Int, topGenome: Genome) {
        if (TrainingUtils.ADAPTIVE_BUDGET) {
            budget = if (BUDGET_EPOCH_STEP > 0)
                BudgetUtils.adaptBudget(epoch, budget)
            else BudgetUtils.adaptBudgetByConditionNeat(budget, topGenome)
        }
    }

    companion object {
        private val neatBestFile = File("$ROOT_OUTPUT_FOLDER/best_genome")
        private val tmpBestFile = File("$ROOT_OUTPUT_FOLDER/tmp_best_genome")

        fun train(gameSettings: GameSettings) {

            println(getActiveAIS() + " population = ${TrainingUtils.POPULATION}" +
                    " units = ${TrainingUtils.HIDDEN_UNITS}")

            val fitnessSum = mutableMapOf<Int, Float>()

            repeat(RUNS) {
                val neatRunner = NeatRunner(gameSettings)
                val pool = Pool().apply { initializePool() }
                //var topGenome = Genome()

                var epochTime = 0.0

                repeat(EPOCH_COUNT) repeat@{ epoch ->
                    epochTime += measureNanoTime {
                        pool.evaluateFitness(neatRunner)
                        //topGenome = pool.topGenome
                        writeEverywhere("$epoch BEST: " + "${neatRunner.topGenome?.points}, wins ${neatRunner.topGenomeWins}")

                        if (!fitnessSum.containsKey(epoch)) fitnessSum[epoch] = 0F
                        fitnessSum[epoch] = fitnessSum[epoch]!! + neatRunner.topGenome!!.points

                        pool.breedNewGeneration()

                        if (epoch % SAVE_POPULATION_INTERVAL == 0) {
                            writeToFile(Gson().toJson(neatRunner.topGenome).toString(), tmpBestFile)
                            savePopulation(pool.species)
                        }

                        if (neatRunner.topGenome != null && neatRunner.topGenomeWins >= getActiveAIS().size) {

                            if (neatRunner.topGenome!!.points >= TRESHOLD_FITNESS &&
                                    (BEST_AI_EPOCH <= epoch || neatRunner.budget >= (BUDGET_UPPER_LIMIT / 2))
                                    && epoch % SAVE_POPULATION_INTERVAL == 0) {

                                val eval = testBest(gameSettings, neatRunner.topGenome!!)
                                if (eval.second == (10 * getActiveAIS().size)) {
                                    savePopulation(pool.species)
                                    writeToFile(Gson().toJson(neatRunner.topGenome).toString(), neatBestFile)
                                    writeEverywhere("Found the topGenome, average fitness ${eval.first}")
                                }
                            }

                            neatRunner.adaptBudget(epoch, neatRunner.topGenome!!)
                        }
                    }
                }

                writeEverywhere("Time per one epoch (avg): ${epochTime / EPOCH_COUNT}")

                writeToFile(Gson().toJson(neatRunner.topGenome).toString(), neatBestFile)
                writeToFile(Gson().toJson(neatRunner.bestGenome).toString(), neatBestFile)

                savePopulation(pool.species)
            }

            fitnessSum.forEach {
                writeToFile("${it.key} BEST ${it.value / RUNS}", averageBestFile)
            }
        }

        private fun testBest(gameSettings: GameSettings, topGenome: Genome): Pair<Double, Int> {
            val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
                TrainingUtils.getFitness(g, a, p, null) }
            return gameRunner.runGameForAIs(GameRunner.getEvaluateLambda(topGenome), getActiveAIS(),
                    print = true, BUDGET_UPPER_LIMIT, runsPerAi = 5)
        }

        private fun savePopulation(species: ArrayList<Species>) {
            popListFile.delete()
            species.forEach {
                it.genomes.forEach { genome ->
                    writeToFile(Gson().toJson(genome).toString(), popListFile)
                }
            }
        }
    }
}