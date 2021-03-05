package ai.evolution

import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils.ACTIVE_START
import ai.evolution.decisionMaker.TrainingUtils.getActiveAIS
import ai.evolution.decisionMaker.TrainingUtils.getPassiveAIS
import ai.evolution.operators.Crossover
import ai.evolution.operators.Fitness
import ai.evolution.operators.Initialisation
import ai.evolution.operators.Selection
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

open class GeneticTrainingAI(gameSettings: GameSettings) : TrainingAI(gameSettings) {

    override fun initialisePopulation(): MutableList<UnitDecisionMaker> =
            Initialisation.simpleInit()

    override fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> =
            Fitness.basicFitness(game, playerStats, player, epoch)

    override fun crossover(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> =
            Crossover.tournament(candidatesFitnessList)

    override fun selection(candidatesFitnessList: MutableList<Utils.Companion.UnitCandidate>,
                           childrenFitnessList: MutableList<Utils.Companion.UnitCandidate>): MutableList<UnitDecisionMaker> =
            Selection.selectBestPopulation(candidatesFitnessList, childrenFitnessList)

    override fun getTrainingAIs(epoch: Int): List<String> =
            if (epoch < ACTIVE_START) getPassiveAIS() else getActiveAIS()
}