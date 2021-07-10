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
import ai.evolution.utils.TrainingUtils.getTrainingAI
import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.utils.BudgetUtils
import ai.evolution.utils.TrainingUtils.BUDGET_UPPER_LIMIT
import ai.evolution.utils.TrainingUtils.EPOCH_COUNT
import com.google.gson.Gson

/**
 * Trains Genetic programming model, using [TrainingAI] with specified genetic operators and training parameters.
 */
class GPTrainingRunner(val ai: TrainingAI) {

    private var candidatesFitnessList = mutableListOf<UnitCandidate>()
    private var childrenFitnessList = mutableListOf<UnitCandidate>()
    private var averageBestFitness = mutableListOf<Double>() // for multiple run

    /**
     * Applies only to AIs with computational budget.
     */
    private var budget = BUDGET_INITIAL

    fun train() {
        repeat(TrainingUtils.RUNS) {
            writeEverywhere("\nRun $it Budget = Init $BUDGET_INITIAL, step $BUDGET_ADAPT_CONSTANT")
            writeEverywhere(TrainingUtils.printInfo())
            writeEverywhere(getTrainingAI().toString())

            var candidates = ai.initialisePopulation()

            for (epoch in 0 until EPOCH_COUNT) {
                writeEverywhere("Epoch $epoch")
                candidatesFitnessList = ai.evaluateFitness(candidates, epoch, false, budget)

                val best = candidatesFitnessList.maxByOrNull { it.fitness; it.wins }
                writeEverywhere("--- BEST: ${best?.fitness} wins: ${best?.wins} budget: $budget")

                if (averageBestFitness.size >= epoch + 1)
                    averageBestFitness[epoch] += best!!.fitness
                else averageBestFitness.add(best!!.fitness)

                val children = ai.crossover(candidatesFitnessList) // Crossover between candidates and mutate children
                childrenFitnessList = ai.evaluateFitness(children, epoch, true, budget) // Evaluate children

                if (epoch >= TrainingUtils.BEST_AI_EPOCH && (!ADAPTIVE_BUDGET || budget >= BUDGET_UPPER_LIMIT) &&
                        ai.saveBestIfFound(candidatesFitnessList)) break

                candidates = ai.selection(candidatesFitnessList, childrenFitnessList) // Selection

                adaptBudget(epoch)
                savePopulation(epoch, candidates)
            }
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