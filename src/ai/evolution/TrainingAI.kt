package ai.evolution

import ai.evolution.utils.Utils.Companion.writeEverywhere
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.BUDGET_INITIAL
import ai.evolution.utils.TrainingUtils.TESTING_INTERVAL
import ai.evolution.utils.TrainingUtils.TESTING_WHILE_TRAINING
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.runners.GameRunner
import ai.evolution.runners.TestingRunner
import ai.evolution.utils.Utils.Companion.UnitCandidate
import ai.evolution.utils.TrainingUtils.BEST_LIST_SIZE
import ai.evolution.utils.TrainingUtils.TRESHOLD_FITNESS
import ai.evolution.utils.Utils
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

abstract class TrainingAI(val gameSettings: GameSettings) {

    var bestCandidate: UnitCandidate? = null

    var bestCandidateList = mutableListOf<UnitCandidate>()

    private val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
        calculateFitness(g, a, p) }

    abstract fun initialisePopulation(): MutableList<UnitDecisionMaker>

    abstract fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int = 1, epoch: Int? = null): Pair<Double, Boolean>

    abstract fun crossover(candidatesFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker>

    abstract fun selection(candidatesFitnessList: MutableList<UnitCandidate>,
                           childrenFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker>

    abstract fun getTrainingAIs(epoch: Int): List<String>

    /*val bestDecisionMaker = bestCandidate?.decisionMaker
        if (bestDecisionMaker != null && epoch >= TOURNAMENT_START) {
            AIs.add(GeneticAI(bestDecisionMaker))
        }*/

    fun evaluateFitness(candidates: MutableList<UnitDecisionMaker>, epoch: Int, children: Boolean = false, budget: Int = BUDGET_INITIAL): MutableList<Utils.Companion.UnitCandidate> {
        val evaluatedCandidates = mutableListOf<UnitCandidate>()
        val ais = getTrainingAIs(epoch)

        prepareCandidates(candidates, children)
                .chunked(TrainingUtils.CORES_COUNT)
                .forEach { list ->
            list.parallelStream().forEach {
                val fitnessEval = gameRunner.runGameForAIs(GameRunner.getEvaluateLambda(it),
                        ais, false, budget, runsPerAi = 1)
                evaluatedCandidates.add(Utils.Companion.UnitCandidate(it, fitnessEval.first, fitnessEval.second))
            }
        }
        evaluatedCandidates.sortByDescending{ it.fitness; it.wins }
        val best = evaluatedCandidates[0]

        with(bestCandidateList) {
            add(best)
            sortByDescending { it.wins; it.fitness }
            if (size > BEST_LIST_SIZE)
                bestCandidateList.removeAt(bestCandidateList.size - 1)
        }

        if (bestCandidate == null || bestCandidate!!.fitness <= best.fitness && epoch >= TrainingUtils.ACTIVE_START) {
            bestCandidate = best
        }

        if (TESTING_WHILE_TRAINING && bestCandidate != null && !children &&
                epoch > 0 && epoch % TESTING_INTERVAL == 0) {
            writeEverywhere("\nEPOCH ${epoch}, FITNESS ${bestCandidate?.fitness}, " +
                    "WINS ${bestCandidate?.wins} ", Utils.evalFile)
            TestingRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
                calculateFitness(g, a, p) }.testAI(GameRunner.getEvaluateLambda(bestCandidate!!.unitDecisionMaker))
            writeEverywhere("\n", Utils.evalFile)
        }

        return prepareEvaluatedCandidates(evaluatedCandidates, children)
    }

    open fun prepareEvaluatedCandidates(evaluatedCandidates: MutableList<UnitCandidate>, children: Boolean): MutableList<UnitCandidate> = evaluatedCandidates

    /**
     * Prepare candidates before running game. Do nothing on default.
     */
    open fun prepareCandidates(candidates: MutableList<UnitDecisionMaker>, children: Boolean): MutableList<UnitDecisionMaker> = candidates

    /**
     * Saves best candidate to file.
     */
    fun saveBestIfFound(candidatesFitnessList: MutableList<UnitCandidate>): Boolean {
        val best = bestCandidate ?: candidatesFitnessList[0]
        if (best.wins >= TrainingUtils.getActiveAIS().size && best.fitness >= TRESHOLD_FITNESS) {
            Utils.writeToFile("Found best unit of Fitness: " + "${best.fitness}")

            // Save best unit to file
            Utils.conditionsFile.delete() // delete existing file first
            Utils.writeToFile(Gson().toJson(best.unitDecisionMaker).toString(), Utils.conditionsFile)

            // Also, save best list if available
            bestCandidateList.forEach {
                Utils.writeToFile(Gson().toJson(it.unitDecisionMaker).toString(), Utils.bestListFile)
            }
            return true
        }
        return false
    }
}