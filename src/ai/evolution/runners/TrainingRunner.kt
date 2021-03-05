package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.Utils
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils

class TrainingRunner(val ai: TrainingAI) {

    private var candidates = mutableListOf<UnitDecisionMaker>()
    private var candidatesFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var childrenFitnessList = mutableListOf<Utils.Companion.UnitCandidate>()
    private var averageBestFitness = mutableListOf<Double>() // for multiple run

    fun train() {
        repeat(TrainingUtils.RUNS) {
            Utils.writeEverywhere("\nRun $it")
            Utils.writeToFile(TrainingUtils.printInfo())
            candidates = ai.initialisePopulation()

            for (epoch in 0 until TrainingUtils.EPOCH_COUNT) {
                Utils.writeEverywhere("Epoch $epoch")
                candidatesFitnessList = ai.evaluateFitness(candidates, epoch) // Evaluate fitness of candidates
                printFitnessStats()

                if (averageBestFitness.size >= epoch + 1)
                    averageBestFitness[epoch] += candidatesFitnessList[0].fitness
                else averageBestFitness.add(candidatesFitnessList[0].fitness)

                val children = ai.crossover(candidatesFitnessList) // Crossover between candidates and mutate children
                childrenFitnessList = ai.evaluateFitness(children, epoch, true) // Evaluate children

                Utils.writeToFile("--- CHILDREN: ${childrenFitnessList.sumByDouble { it.fitness } / childrenFitnessList.size} " +
                        "wins: ${childrenFitnessList.sumByDouble { it.wins.toDouble() } / childrenFitnessList.size}")

                if (epoch >= TrainingUtils.BEST_AI_EPOCH && ai.saveBestIfFound(candidatesFitnessList)) break

                candidates = ai.selection(candidatesFitnessList, childrenFitnessList) // Selection
            }
        }

        // Print fitness from multiple runs
        averageBestFitness.take(TrainingUtils.BEST_AI_EPOCH).forEach {
            Utils.writeToFile("--- BEST: ${it / TrainingUtils.RUNS}", Utils.averageBestFile)
        }
    }

    private fun printFitnessStats() {
        Utils.writeEverywhere("--- BEST: ${candidatesFitnessList[0].fitness} wins: ${candidatesFitnessList[0].wins}")
        Utils.writeToFile("--- WORST: ${candidatesFitnessList[candidatesFitnessList.size - 1].fitness}")
        Utils.writeToFile("--- AVG: ${
            candidatesFitnessList.sumByDouble { it.fitness } /
                    candidatesFitnessList.size
        } wins: ${candidatesFitnessList.sumByDouble { it.wins.toDouble() } / candidatesFitnessList.size}")
    }
}