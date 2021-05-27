package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.Utils
import ai.evolution.Utils.Companion.popListFile
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.ACTIVE_START
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_ADAPT_CONSTANT
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_INITIAL
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_EPOCH_STEP
import ai.evolution.decisionMaker.TrainingUtils.SAVE_POPULATION_INTERVAL
import ai.evolution.decisionMaker.UnitDecisionMaker
import com.google.gson.Gson

class TrainingRunner(val ai: TrainingAI) {

    private var candidatesFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var childrenFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var averageBestFitness = mutableListOf<Double>() // for multiple run

    private var budget = BUDGET_INITIAL

    fun train() {
        repeat(TrainingUtils.RUNS) {
            Utils.writeEverywhere("\nRun $it")
            Utils.writeEverywhere(TrainingUtils.printInfo())
            Utils.writeEverywhere(TrainingUtils.getActiveAIS().toString())

            var candidates = ai.initialisePopulation()

            for (epoch in 0 until TrainingUtils.EPOCH_COUNT) {
                Utils.writeEverywhere("Epoch $epoch")
                candidatesFitnessList = ai.evaluateFitness(candidates, epoch, false, budget)

                val best = candidatesFitnessList.maxByOrNull { it.fitness; it.wins }
                Utils.writeEverywhere("--- BEST: ${best?.fitness} wins: ${best?.wins}")

                if (averageBestFitness.size >= epoch + 1)
                    averageBestFitness[epoch] += best!!.fitness
                else averageBestFitness.add(best!!.fitness)

                val children = ai.crossover(candidatesFitnessList) // Crossover between candidates and mutate children
                childrenFitnessList = ai.evaluateFitness(children, epoch, true, budget) // Evaluate children

                if (epoch >= TrainingUtils.BEST_AI_EPOCH &&
                        ai.saveBestIfFound(candidatesFitnessList)) break

                candidates = ai.selection(candidatesFitnessList, childrenFitnessList) // Selection

                adaptBudget(epoch)
                if (epoch % SAVE_POPULATION_INTERVAL == 0)
                    savePopulation(candidates)
            }
        }

        // Print fitness from multiple runs
        averageBestFitness.take(TrainingUtils.BEST_AI_EPOCH).forEach {
            Utils.writeToFile("--- BEST: ${it / TrainingUtils.RUNS}", Utils.averageBestFile)
        }
    }

    private fun savePopulation(candidates: MutableList<UnitDecisionMaker>) {
        popListFile.delete()
        candidates.forEach { writeToFile(Gson().toJson(it).toString(), popListFile) }
    }

    private fun adaptBudget(epoch: Int) {
        if (BUDGET_EPOCH_STEP <= 0) return
        if (ACTIVE_START > 0 && ACTIVE_START == epoch) {
            budget = BUDGET_INITIAL
            ai.bestCandidate = null
            Utils.writeEverywhere("//Current budget: $budget")
        }
        else if (epoch % BUDGET_EPOCH_STEP == 0 && epoch > 0 && budget < 100) {
            budget += BUDGET_ADAPT_CONSTANT
            ai.bestCandidate = null // delete current best candidate, because param has changed
            Utils.writeEverywhere("//Current budget: $budget")
        }
    }
}