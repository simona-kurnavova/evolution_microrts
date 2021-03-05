package ai.evolution

import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils.ACTIVE_START
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.decisionMaker.TrainingUtils.getActiveAIS
import ai.evolution.decisionMaker.TrainingUtils.getPassiveAIS
import ai.evolution.operators.Crossover
import ai.evolution.operators.Fitness
import ai.evolution.operators.Initialisation
import ai.evolution.operators.Selection
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

/**
 * Saves internally strategies and evolve them independent of unit AIs.
 */
open class GeneticStrategyComplexTrainingAI(gameSettings: GameSettings) : TrainingAI(gameSettings) {

    var strategies = mutableListOf<StrategyDecisionMaker>()
    var children = mutableListOf<StrategyDecisionMaker>()
    var strategyFitnessList = mutableListOf<Utils.Companion.StrategyCandidate>()
    var childrenStrategyList = mutableListOf<Utils.Companion.StrategyCandidate>()

    init {
        println("GeneticStrategyComplexTrainingAI")
    }

    override fun initialisePopulation(): MutableList<UnitDecisionMaker> {
        repeat(StrategyTrainingUtils.POPULATION) {
            strategies.add(StrategyDecisionMaker(StrategyTrainingUtils.CONDITION_COUNT))
        }
        return Initialisation.simpleInit()
    }

    override fun prepareCandidates(candidates: MutableList<UnitDecisionMaker>, children: Boolean): MutableList<UnitDecisionMaker> {
        // Adds strategy AIs to candidates before running the game
        candidates.shuffled().chunked(POPULATION / StrategyTrainingUtils.POPULATION).forEachIndexed { index, decisionMakers ->
            decisionMakers.forEach {
                it.strategyDecisionMaker = if (children) this.children[index] else strategies[index]
            }
        }
        return candidates
    }

    override fun prepareEvaluatedCandidates(evaluatedCandidates: MutableList<Utils.Companion.UnitCandidate>, children: Boolean): MutableList<Utils.Companion.UnitCandidate> {
        val fitnessList = mutableListOf<Utils.Companion.StrategyCandidate>()

        evaluatedCandidates.groupBy { it.unitDecisionMaker.strategyDecisionMaker }.forEach { (_, u) ->

            val strategyDecisionMaker = u[0].unitDecisionMaker.strategyDecisionMaker

            assert(strategyDecisionMaker != null)
            if (strategyDecisionMaker != null) {
                fitnessList.add(Utils.Companion.StrategyCandidate(strategyDecisionMaker,
                    u.sumByDouble { it.fitness } / u.size, u.sumBy { it.wins } / u.size))
                u.forEach { it.unitDecisionMaker.strategyDecisionMaker = null }
            }
        }
        if (children) childrenStrategyList = fitnessList else strategyFitnessList = fitnessList
        return evaluatedCandidates
    }

    override fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> =
            Fitness.basicFitness(game, playerStats, player, epoch)

    override fun crossover(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> {
        children = Crossover.strategyTournament(strategyFitnessList)
        return Crossover.tournament(candidatesFitnessList)
    }


    override fun selection(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>,
                           childrenFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> {
        strategies = Selection.selectBestStrategyPopulation(strategyFitnessList, childrenStrategyList)
        return Selection.selectBestPopulation(candidatesFitnessList, childrenFitnessList)
    }

    override fun getTrainingAIs(epoch: Int): List<String> =
            if (epoch < ACTIVE_START) getPassiveAIS() else getActiveAIS()
}