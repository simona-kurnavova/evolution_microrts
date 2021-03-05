package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.Utils
import ai.evolution.Utils.Companion.conditionsFile
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import java.io.File

class TestingRunner(val ai: TrainingAI) {

    private val gameRunner = GameRunner(ai.gameSettings)
    { g: Game, a: ActionStatistics, p: Int -> ai.calculateFitness(g, a, p) }

    /**
     * Parses unit from specified file and evaluates it.
     */
    fun evaluateUnitFromFile(file: File = conditionsFile) {
        // Run tests
        testBestAI(loadAIFromFile(file))
    }

    fun runAIFromFile(file: File = conditionsFile) {
        val fitness = gameRunner.runGame(gameRunner.getEvaluateLambda(loadAIFromFile(file)), TrainingUtils.getFastTestingAIs()[0], 0)
        if(fitness != null) {
            println("Result: ${fitness.first}, won=${fitness.second}")
        }
    }

    private fun testBestAI(unitDecisionMaker: UnitDecisionMaker) {
        val ais = TrainingUtils.getFastTestingAIs()
        if (TrainingUtils.SLOW_TESTING) ais.addAll(TrainingUtils.getSlowTestingAIs())
        Utils.writeToFile(Gson().toJson(unitDecisionMaker).toString(), Utils.evalFile)
        gameRunner.runGameForAIs(gameRunner.getEvaluateLambda(unitDecisionMaker), ais, TrainingUtils.TESTING_RUNS, true)
    }

    private fun loadAIFromFile(file: File = conditionsFile): UnitDecisionMaker {
        val text = file.readText(Charsets.UTF_8).replace("\\s+".toRegex(), " ") // read whole file and remove whitespaces
        return Gson().fromJson(text, UnitDecisionMaker::class.java) // parse to DecisionMaker class
    }
}