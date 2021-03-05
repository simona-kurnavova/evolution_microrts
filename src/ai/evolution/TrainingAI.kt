package ai.evolution

import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.runners.GameRunner
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

abstract class TrainingAI(val gameSettings: GameSettings) {

    private var bestCandidate: Utils.Companion.UnitCandidate? = null

    private val gameRunner = GameRunner(gameSettings) { g: Game, a: ActionStatistics, p: Int ->
        calculateFitness(g, a, p) }

    abstract fun initialisePopulation(): MutableList<UnitDecisionMaker>

    abstract fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int = 1, epoch: Int? = null): Pair<Double, Boolean>

    abstract fun crossover(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker>

    abstract fun selection(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>,
                           childrenFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker>

    abstract fun getTrainingAIs(epoch: Int): List<String>

    /*val bestDecisionMaker = bestCandidate?.decisionMaker
        if (bestDecisionMaker != null && epoch >= TOURNAMENT_START) {
            AIs.add(GeneticAI(bestDecisionMaker))
        }*/

    fun evaluateFitness(candidates: MutableList<UnitDecisionMaker>, epoch: Int, children: Boolean = false): MutableList<Utils.Companion.UnitCandidate> {
        val evaluatedCandidates = mutableListOf<Utils.Companion.UnitCandidate>()
        val ais = getTrainingAIs(epoch)

        prepareCandidates(candidates, children).chunked(TrainingUtils.CORES_COUNT).forEach { list ->
            list.parallelStream().forEach {
                val fitnessEval = gameRunner.runGameForAIs(gameRunner.getEvaluateLambda(it), ais)
                evaluatedCandidates.add(Utils.Companion.UnitCandidate(it, fitnessEval.first, fitnessEval.second))
            }
        }
        evaluatedCandidates.sortByDescending{ it.wins; it.fitness }

        if (bestCandidate == null || bestCandidate!!.fitness <= evaluatedCandidates[0].fitness && epoch >= TrainingUtils.ACTIVE_START) {
            bestCandidate = evaluatedCandidates[0]
        }
        return prepareEvaluatedCandidates(evaluatedCandidates, children)
    }

    open fun prepareEvaluatedCandidates(evaluatedCandidates: MutableList<Utils.Companion.UnitCandidate>, children: Boolean): MutableList<Utils.Companion.UnitCandidate> = evaluatedCandidates

    /**
     * Prepare candidates before running game. Do nothing on default.
     */
    open fun prepareCandidates(candidates: MutableList<UnitDecisionMaker>, children: Boolean): MutableList<UnitDecisionMaker> = candidates

    /**
     * Saves best candidate to file.
     */
    fun saveBestIfFound(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): Boolean {
        val best = bestCandidate ?: candidatesFitnessList[0]
        if (best.wins >= TrainingUtils.getActiveAIS().size) {
            Utils.writeToFile("Found best unit of Fitness: " + "${best.fitness}")

            // Save best unit to file
            Utils.conditionsFile.delete() // delete existing file first
            Utils.writeToFile(Gson().toJson(best.unitDecisionMaker).toString(), Utils.conditionsFile)
            return true
        }
        return false
    }
}