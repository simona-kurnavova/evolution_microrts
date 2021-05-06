package ai.evolution.runners

import ai.evolution.Utils.Companion.conditionsFile
import ai.evolution.decisionMaker.AbstractAction
import ai.evolution.decisionMaker.State
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.AI
import ai.evolution.decisionMaker.TrainingUtils.getFastTestingAIs
import ai.evolution.neat.Genome
import ai.evolution.strategyDecisionMaker.GlobalState
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import java.io.File

class TestingRunner(gameSettings: GameSettings,
                    val fitness: (Game, ActionStatistics, Int) -> Pair<Double, Boolean>) {

    private val gameRunner = GameRunner(gameSettings, fitness)

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
                println("\n${it}:")
                val wins = fastTestAI(loadAIFromFile(file, it))
                if (wins > bestWins) {
                    bestWins = wins
                    best = it
                }
            }
            println("Best is wins = $bestWins, index = $best\n\n")
        }
        testAI(loadAIFromFile(file, best))
    }

    fun runAIFromFile(file: File = conditionsFile) {
        val fitness = gameRunner.runGame(loadAIFromFile(file), TrainingUtils.getTestingAIs()[0], 0)
        if(fitness != null) {
            println("Result: ${fitness.first}, won=${fitness.second}")
        }
    }

    private fun fastTestAI(decider: (State, GlobalState) -> List<AbstractAction>): Int =
            gameRunner.runGameForAIs(decider, getFastTestingAIs(), true, budget = 10, runsPerAi = 10).second

    fun testAI(decider: (State, GlobalState) -> List<AbstractAction>) {
        val ais = TrainingUtils.getTestingAIs()
        gameRunner.runGameForAIs(decider, ais, true)
    }

    companion object {

        fun loadAIFromFile(file: File, index: Int = 0): (State, GlobalState) -> List<AbstractAction> {
            val lines = file.readLines(Charsets.UTF_8)
            val text = lines[index].replace("\\s+".toRegex(), " ") // read whole file and remove whitespaces

            return if (AI == TrainingUtils.TrainAI.NEAT)
                GameRunner.getEvaluateLambda(Gson().fromJson(text, Genome::class.java))
                else GameRunner.getEvaluateLambda(Gson().fromJson(text, UnitDecisionMaker::class.java)) // parse to DecisionMaker or genome class
        }

        fun loadDecisionMakerFromFile(file: File, index: Int = 0): UnitDecisionMaker {
            val lines = file.readLines(Charsets.UTF_8)
            val text = lines[index].replace("\\s+".toRegex(), " ") // read whole file and remove whitespaces
            return Gson().fromJson(text, UnitDecisionMaker::class.java)
        }
    }
}