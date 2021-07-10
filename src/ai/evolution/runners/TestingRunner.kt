package ai.evolution.runners

import ai.evolution.utils.Utils.Companion.conditionsFile
import ai.evolution.state.AbstractAction
import ai.evolution.state.State
import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.AI
import ai.evolution.evoneat.Genome
import ai.evolution.gpstrategy.GlobalState
import ai.evolution.utils.TestingUtils
import ai.evolution.utils.TestingUtils.TESTING_POP_BUDGET
import ai.evolution.utils.TestingUtils.TESTING_POP_RUNS
import ai.evolution.utils.TestingUtils.getFastTestingAIs
import ai.evolution.utils.TestingUtils.getTestingAIs
import ai.evolution.utils.TrainingUtils.getTrainingAI
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import java.io.File
import kotlin.math.abs

/**
 * Runs test simulations.
 */
class TestingRunner(gameSettings: GameSettings,
                    val fitness: (Game, ActionStatistics, Int) -> Pair<Double, Boolean>) {

    private val gameRunner = GameRunner(gameSettings, fitness)

    fun evaluateByTournament() {
        val ai1 = loadAIFromFile(TestingUtils.AI_1, 0, TestingUtils.AI_1_TYPE)
        val ai2 = loadAIFromFile(TestingUtils.AI_2, 0, TestingUtils.AI_2_TYPE)

        var player = 0
        val fitnesses = mutableListOf<Pair<Double, Boolean>?>()

        repeat(TestingUtils.TESTING_RUNS) {
            player = abs(player - 1)
            fitnesses.add(gameRunner.runTournament(ai1, ai2))
        }

        println("Player 1 AVG fitness: ${fitnesses.sumByDouble { it?.first ?: 0.0 } / fitnesses.size}")
        println("Wins: ${fitnesses.count { it != null && it.second }}")
    }

    /**
     * Parses unit from specified [file] and evaluates it.
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
        val fitness = gameRunner.runGame(loadAIFromFile(file), getTestingAIs()[0], 0)
        if(fitness != null) {
            println("Result: ${fitness.first}, won=${fitness.second}")
        }
    }

    fun testAI(decider: (State, GlobalState) -> List<AbstractAction>) {
        val ais = getTestingAIs()
        gameRunner.runGameForAIs(decider, ais, true)
    }

    private fun fastTestAI(decider: (State, GlobalState) -> List<AbstractAction>): Int =
            gameRunner.runGameForAIs(decider, getFastTestingAIs(), true, budget = TESTING_POP_BUDGET, runsPerAi = TESTING_POP_RUNS).second

    companion object {

        fun loadAIFromFile(file: File, index: Int = 0, ai: TrainingUtils.TrainAI = AI): (State, GlobalState) -> List<AbstractAction> {
            val lines = file.readLines(Charsets.UTF_8)
            val text = lines[index].replace("\\s+".toRegex(), " ") // read whole file and remove whitespaces

            return if (ai == TrainingUtils.TrainAI.NEAT)
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