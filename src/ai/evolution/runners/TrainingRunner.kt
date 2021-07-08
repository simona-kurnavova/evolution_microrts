package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.utils.Utils
import ai.evolution.utils.Utils.Companion.UnitCandidate
import ai.evolution.utils.Utils.Companion.popListFile
import ai.evolution.utils.Utils.Companion.writeEverywhere
import ai.evolution.utils.Utils.Companion.writeToFile
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.ADAPTIVE_BUDGET
import ai.evolution.utils.TrainingUtils.BUDGET_ADAPT_CONSTANT
import ai.evolution.utils.TrainingUtils.BUDGET_INITIAL
import ai.evolution.utils.TrainingUtils.BUDGET_EPOCH_STEP
import ai.evolution.utils.TrainingUtils.SAVE_POPULATION_INTERVAL
import ai.evolution.utils.TrainingUtils.getActiveAIS
import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.utils.BudgetUtils
import ai.evolution.utils.TrainingUtils.EPOCH_COUNT
import com.google.gson.Gson
import kotlin.system.measureNanoTime

class TrainingRunner(val ai: TrainingAI) {

    private var candidatesFitnessList = mutableListOf<UnitCandidate>()
    private var childrenFitnessList = mutableListOf<UnitCandidate>()
    private var averageBestFitness = mutableListOf<Double>() // for multiple run

    private var budget = BUDGET_INITIAL

    fun train() {
        repeat(TrainingUtils.RUNS) {
            writeEverywhere("\nRun $it Budget = Init $BUDGET_INITIAL, step $BUDGET_ADAPT_CONSTANT")
            writeEverywhere(TrainingUtils.printInfo())
            writeEverywhere(getActiveAIS().toString())

            var candidates = ai.initialisePopulation()
            var epochTime = 0.0

            for (epoch in 0 until EPOCH_COUNT) {
                epochTime += measureNanoTime {
                    writeEverywhere("Epoch $epoch")
                    candidatesFitnessList = ai.evaluateFitness(candidates, epoch, false, budget)

                    val best = candidatesFitnessList.maxByOrNull { it.fitness; it.wins }
                    writeEverywhere("--- BEST: ${best?.fitness} wins: ${best?.wins} budget: $budget")

                    if (averageBestFitness.size >= epoch + 1)
                        averageBestFitness[epoch] += best!!.fitness
                    else averageBestFitness.add(best!!.fitness)

                    val children = ai.crossover(candidatesFitnessList) // Crossover between candidates and mutate children
                    childrenFitnessList = ai.evaluateFitness(children, epoch, true, budget) // Evaluate children

                    //if (epoch >= TrainingUtils.BEST_AI_EPOCH && (!ADAPTIVE_BUDGET || budget >= BUDGET_UPPER_LIMIT) &&
                    //        ai.saveBestIfFound(candidatesFitnessList)) break

                    candidates = ai.selection(candidatesFitnessList, childrenFitnessList) // Selection

                    adaptBudget(epoch)
                    savePopulation(epoch, candidates)
                }
            }
            println("Epoch time on AVG is ${epochTime / EPOCH_COUNT}")
        }

        // Print fitness from multiple runs
        averageBestFitness.take(TrainingUtils.BEST_AI_EPOCH).forEach {
            writeToFile("--- BEST: ${it / TrainingUtils.RUNS}", Utils.averageBestFile)
        }
    }

    private fun savePopulation(epoch: Int, candidates: MutableList<UnitDecisionMaker>) {
        if (epoch % SAVE_POPULATION_INTERVAL == 0) {
            popListFile.delete()
            candidates.forEach { writeToFile(Gson().toJson(it).toString(), popListFile) }
        }
    }

    private fun adaptBudget(epoch: Int) {
        if (ADAPTIVE_BUDGET) {
            budget = if(BUDGET_EPOCH_STEP > 0)
                BudgetUtils.adaptBudget(epoch, budget) { ai.bestCandidate = null }
            else BudgetUtils.adaptBudgetByConditionSga(budget, candidatesFitnessList)
        }
    }
}