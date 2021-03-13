package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.Utils
import ai.evolution.decisionMaker.TrainingUtils

class TrainingRunner(val ai: TrainingAI) {

    private var candidatesFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var childrenFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var averageBestFitness = mutableListOf<Double>() // for multiple run

    fun train() {
        repeat(TrainingUtils.RUNS) {
            Utils.writeEverywhere("\nRun $it")
            Utils.writeToFile(TrainingUtils.printInfo())
            var candidates = ai.initialisePopulation()

            for (epoch in 0 until TrainingUtils.EPOCH_COUNT) {
                Utils.writeEverywhere("Epoch $epoch")
                candidatesFitnessList = ai.evaluateFitness(candidates, epoch)

                val best = candidatesFitnessList.maxByOrNull { it.fitness; it.wins }
                Utils.writeEverywhere("--- BEST: ${best?.fitness} wins: ${best?.wins}")

                if (averageBestFitness.size >= epoch + 1)
                    averageBestFitness[epoch] += best!!.fitness
                else averageBestFitness.add(best!!.fitness)

                val children = ai.crossover(candidatesFitnessList) // Crossover between candidates and mutate children
                childrenFitnessList = ai.evaluateFitness(children, epoch, true) // Evaluate children

                if (epoch >= TrainingUtils.BEST_AI_EPOCH && ai.saveBestIfFound(candidatesFitnessList)) break

                candidates = ai.selection(candidatesFitnessList, childrenFitnessList) // Selection
            }
        }

        // Print fitness from multiple runs
        averageBestFitness.take(TrainingUtils.BEST_AI_EPOCH).forEach {
            Utils.writeToFile("--- BEST: ${it / TrainingUtils.RUNS}", Utils.averageBestFile)
        }
    }
}