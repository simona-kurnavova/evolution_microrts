package gui.frontend

import ai.evolution.GeneticTrainingAI
import rts.GameSettings

class TrainingUI {
    
    companion object {
        fun main(gameSettings: GameSettings) {
            train(gameSettings)
            test(gameSettings)
        }

        fun train(gameSettings: GameSettings) = GeneticTrainingAI(gameSettings).train()

        fun test(gameSettings: GameSettings) = GeneticTrainingAI(gameSettings).evaluateUnitFromFile()

        fun runGame(gameSettings: GameSettings) = GeneticTrainingAI(gameSettings).runAIFromFile()
    }
}