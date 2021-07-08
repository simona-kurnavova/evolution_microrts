package gui.frontend

import ai.evolution.GeneticStrategyComplexTrainingAI
import ai.evolution.GeneticStrategyTrainingAI
import ai.evolution.GeneticTrainingAI
import ai.evolution.TrainingAI
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.MODE
import ai.evolution.utils.TestingUtils.TEST_FILE
import ai.evolution.neat.NeatRunner
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
            println("AI: ${TrainingUtils.AI}, MODE: $MODE")
            if (MODE == TrainingUtils.Mode.TESTING)
                println("File: ${TEST_FILE}")

            if (MODE == TrainingUtils.Mode.TOURNAMENT_TESTING) {
                getNeatTestingRunner(gameSettings).evaluateByTournament()
                return
            }

            if (TrainingUtils.AI == TrainingUtils.TrainAI.NEAT) {
                if (MODE == TrainingUtils.Mode.TESTING)
                    getNeatTestingRunner(gameSettings).evaluateUnitFromFile(TEST_FILE)
                else NeatRunner.train(gameSettings)
            }

            val ai = when(TrainingUtils.AI) {
                TrainingUtils.TrainAI.GP -> GeneticTrainingAI(gameSettings)
                TrainingUtils.TrainAI.GP_STRATEGY -> GeneticStrategyTrainingAI(gameSettings)
                TrainingUtils.TrainAI.COMPLEX_STRATEGY -> GeneticStrategyComplexTrainingAI(gameSettings)
                else -> null
            }
            if (ai != null) {
                if (MODE == TrainingUtils.Mode.TESTING) getTestingRunner(gameSettings, ai).evaluateUnitFromFile(TEST_FILE)
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
            g: Game, a: ActionStatistics, p: Int -> TrainingUtils.getFitness(g, a, p, null)
        }
    }
}