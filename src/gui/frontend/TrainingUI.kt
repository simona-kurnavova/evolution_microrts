package gui.frontend

import ai.evolution.GeneticTrainingAI
import ai.evolution.Utils.Companion.actions
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.Condition
import ai.evolution.condition.DecisionMaker
import ai.evolution.condition.action.AbstractAction
import rts.GameSettings


class TrainingUI {
    
    companion object {
        fun main(gameSettings: GameSettings) {
            val trainingAI = GeneticTrainingAI()
            trainingAI.train(gameSettings)

            /*val dec1 = DecisionMaker()
            val dec2 = DecisionMaker()
            val cond = Condition()

            dec1.addCondition(cond)
            dec2.addCondition(cond)

            writeToFile(dec1.toString())
            writeToFile(dec2.toString())

            dec1.conditions[0].abstractAction.mutate()

            writeToFile("")
            writeToFile(dec1.toString())
            writeToFile(dec2.toString())*/
        }
    }
}