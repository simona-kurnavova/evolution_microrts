package gui.frontend

import ai.evolution.GeneticTrainingAI
import rts.GameSettings


class TrainingUI {
    
    companion object {
        fun main(gameSettings: GameSettings) {
            val trainingAI = GeneticTrainingAI()
            trainingAI.train(gameSettings)
        }
    }
}