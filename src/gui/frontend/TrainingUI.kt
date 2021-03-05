package gui.frontend

import ai.evolution.GeneticStrategyComplexTrainingAI
import ai.evolution.GeneticStrategyTrainingAI
import ai.evolution.GeneticTrainingAI
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.neat.Environment
import ai.evolution.neat.RTSEnvironment
import ai.evolution.runners.TestingRunner
import ai.evolution.runners.TrainingRunner
import rts.GameSettings

class TrainingUI {
    
    companion object {

        fun main(gameSettings: GameSettings) {
            train(gameSettings)
            test(gameSettings)
        }

        fun train(gameSettings: GameSettings) {
            val ai = when(TrainingUtils.AI) {
                TrainingUtils.TrainAI.SIMPLE -> GeneticTrainingAI(gameSettings)
                TrainingUtils.TrainAI.SIMPLE_STRATEGY -> GeneticStrategyTrainingAI(gameSettings)
                TrainingUtils.TrainAI.COMPLEX_STRATEGY -> GeneticStrategyComplexTrainingAI(gameSettings)
            }
            TrainingRunner(ai).train()
        }

        fun test(gameSettings: GameSettings) =
                TestingRunner(GeneticTrainingAI(gameSettings)).evaluateUnitFromFile()

        fun runGame(gameSettings: GameSettings) =
                TestingRunner(GeneticTrainingAI(gameSettings)).runAIFromFile()

        fun trainNEAT(gameSettings: GameSettings) {
            RTSEnvironment.train(gameSettings)
        }
    }
}