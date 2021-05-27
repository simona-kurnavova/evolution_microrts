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
import ai.evolution.Utils.Companion.UnitCandidate
import ai.evolution.Utils.Companion.StrategyCandidate
import ai.evolution.decisionMaker.TrainingUtils
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

/**
 * Saves internally strategies and evolve them independent of unit AIs.
 */
open class GeneticStrategyComplexTrainingAI(gameSettings: GameSettings) : TrainingAI(gameSettings) {

    var strategies = mutableListOf<StrategyDecisionMaker>()
    var children = mutableListOf<StrategyDecisionMaker>()
    var strategyFitnessList = mutableListOf<StrategyCandidate>()
    var childrenStrategyList = mutableListOf<StrategyCandidate>()

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
        val newCandidates = mutableListOf<UnitDecisionMaker>()
        repeat(3) {
            candidates.shuffled().chunked(POPULATION / StrategyTrainingUtils.POPULATION)
                    .forEachIndexed { index, decisionMakers ->
                        decisionMakers.forEach {
                            it.strategyDecisionMaker = if (children) this.children[index]
                            else strategies[index]
                            newCandidates.add(it)
                        }
                    }
        }
        return candidates
    }

    override fun prepareEvaluatedCandidates(evaluatedCandidates: MutableList<UnitCandidate>, children: Boolean): MutableList<UnitCandidate> {
        val fitnessList = mutableListOf<StrategyCandidate>()

        evaluatedCandidates.groupBy { it.unitDecisionMaker.strategyDecisionMaker }.forEach { (_, u) ->

            val strategyDecisionMaker = u[0].unitDecisionMaker.strategyDecisionMaker

            assert(strategyDecisionMaker != null)
            if (strategyDecisionMaker != null) {
                fitnessList.add(Utils.Companion.StrategyCandidate(strategyDecisionMaker,
                    u.sumByDouble { it.fitness } / u.size, u.sumBy { it.wins } / u.size))
                u.forEach { it.unitDecisionMaker.strategyDecisionMaker = null }
            }
        }
        val finalCandidates = mutableListOf<UnitCandidate>()
        evaluatedCandidates.groupBy { it.unitDecisionMaker.conditions }.forEach { (_, u) ->
            u[0].fitness = u.sumByDouble { it.fitness } / u.size
            u[0].wins = u.sumBy { it.wins } / u.size
            finalCandidates.add(u[0])
        }
        if (children) childrenStrategyList = fitnessList else strategyFitnessList = fitnessList
        return finalCandidates
    }

    override fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> =
            TrainingUtils.getFitness(game, playerStats, player, epoch)

    override fun crossover(candidatesFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> {
        children = Crossover.strategyTournament(strategyFitnessList)
        return Crossover.tournament(candidatesFitnessList)
    }


    override fun selection(candidatesFitnessList: MutableList<UnitCandidate>,
                           childrenFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> {
        strategyFitnessList = Selection.selectBestStrategyPopulation(strategyFitnessList, childrenStrategyList)
        return Selection.selectBestPopulation(candidatesFitnessList, childrenFitnessList)
    }

    override fun getTrainingAIs(epoch: Int): List<String> =
            if (epoch < ACTIVE_START) getPassiveAIS() else getActiveAIS()
}