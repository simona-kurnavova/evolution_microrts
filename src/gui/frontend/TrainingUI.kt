package gui.frontend

import ai.evolution.GeneticStrategyComplexTrainingAI
import ai.evolution.GeneticStrategyTrainingAI
import ai.evolution.GeneticTrainingAI
import ai.evolution.TrainingAI
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.TESTING_ONLY_MODE
import ai.evolution.decisionMaker.TrainingUtils.TEST_FILE
import ai.evolution.neat.RTSEnvironment
import ai.evolution.operators.Fitness
import ai.evolution.runners.TestingRunner
import ai.evolution.runners.TrainingRunner
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

class TrainingUI {
    
    companion object {

        fun main(gameSettings: GameSettings) {
            train(gameSettings)
            test(gameSettings)
        }

        fun train(gameSettings: GameSettings) {
            println("AI: ${TrainingUtils.AI}, TESTING_ONLY: $TESTING_ONLY_MODE")

            if (TrainingUtils.AI == TrainingUtils.TrainAI.NEAT) {
                if (TESTING_ONLY_MODE) getNeatTestingRunner(gameSettings).evaluateUnitFromFile(TEST_FILE)
                else RTSEnvironment.train(gameSettings)
            }

            val ai = when(TrainingUtils.AI) {
                TrainingUtils.TrainAI.SIMPLE -> GeneticTrainingAI(gameSettings)
                TrainingUtils.TrainAI.SIMPLE_STRATEGY -> GeneticStrategyTrainingAI(gameSettings)
                TrainingUtils.TrainAI.COMPLEX_STRATEGY -> GeneticStrategyComplexTrainingAI(gameSettings)
                else -> null
            }
            if (ai != null) {
                if (TESTING_ONLY_MODE) getTestingRunner(gameSettings, ai).evaluateUnitFromFile(TEST_FILE)
                else TrainingRunner(ai).train()
            }
        }

        fun test(gameSettings: GameSettings) = getTestingRunner(gameSettings,
                GeneticTrainingAI(gameSettings)).evaluateUnitFromFile()

        fun runGame(gameSettings: GameSettings) = getTestingRunner(gameSettings,
                GeneticTrainingAI(gameSettings)).runAIFromFile()

        private fun getTestingRunner(gameSettings: GameSettings, ai: TrainingAI) =  TestingRunner(gameSettings) {
            g: Game, a: ActionStatistics, p: Int -> ai.calculateFitness(g, a, p)
        }

        private fun getNeatTestingRunner(gameSettings: GameSettings) =  TestingRunner(gameSettings) {
            g: Game, a: ActionStatistics, p: Int -> Fitness.basicFitness(g, a, p, null)
        }
    }
}