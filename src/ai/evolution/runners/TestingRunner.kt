package ai.evolution.runners

import ai.evolution.TrainingAI
import ai.evolution.Utils.Companion.conditionsFile
import ai.evolution.Utils.Companion.evalFile
import ai.evolution.Utils.Companion.writeEverywhere
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.decisionMaker.TrainingUtils.TESTING_BUDGET
import ai.evolution.decisionMaker.TrainingUtils.getFastTestingAIs
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
        val lines = file.readLines(Charsets.UTF_8).size
        var best = 0
        var bestWins = 0

        if (lines > 1) {
            repeat(lines) {
                writeEverywhere("\n${it}:", evalFile)
                val wins = fastTestAI(loadAIFromFile(file, it))
                if (wins > bestWins) {
                    bestWins = wins
                    best = it
                }
            }
            println("Best is wins = $bestWins, index = $best")
        }
        testAI(loadAIFromFile(file, best))
    }

    fun runAIFromFile(file: File = conditionsFile) {
        val fitness = gameRunner.runGame(gameRunner.getEvaluateLambda(loadAIFromFile(file)), TrainingUtils.getTestingAIs()[0], 0)
        if(fitness != null) {
            println("Result: ${fitness.first}, won=${fitness.second}")
        }
    }

    private fun fastTestAI(unitDecisionMaker: UnitDecisionMaker): Int = gameRunner.runGameForAIs(
            gameRunner.getEvaluateLambda(unitDecisionMaker), getFastTestingAIs(), true, budget = 10,
            runsPerAi = 10).second

    fun testAI(unitDecisionMaker: UnitDecisionMaker) {
        val ais = TrainingUtils.getTestingAIs()
        //Utils.writeToFile(Gson().toJson(unitDecisionMaker).toString(), Utils.evalFile)
        gameRunner.runGameForAIs(gameRunner.getEvaluateLambda(unitDecisionMaker), ais, true)
    }

    companion object {
        fun loadAIFromFile(file: File = conditionsFile, index: Int = 0): UnitDecisionMaker {
            val lines = file.readLines(Charsets.UTF_8)
            val text = lines[index].replace("\\s+".toRegex(), " ") // read whole file and remove whitespaces
            return Gson().fromJson(text, UnitDecisionMaker::class.java) // parse to DecisionMaker class
        }
    }
}