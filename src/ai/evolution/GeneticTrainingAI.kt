package ai.evolution

import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.utils.TrainingUtils.ACTIVE_START
import ai.evolution.utils.TrainingUtils.getActiveAIS
import ai.evolution.utils.TrainingUtils.getPassiveAIS
import ai.evolution.utils.Utils.Companion.UnitCandidate
import ai.evolution.operators.Crossover
import ai.evolution.operators.Fitness
import ai.evolution.operators.Initialisation
import ai.evolution.operators.Selection
import rts.ActionStatistics
import rts.Game
import rts.GameSettings

open class GeneticTrainingAI(gameSettings: GameSettings) : TrainingAI(gameSettings) {

    init {
        println("GeneticTrainingAI")
    }

    override fun initialisePopulation(): MutableList<UnitDecisionMaker> =
            Initialisation.simpleInit()

    override fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?): Pair<Double, Boolean> =
            Fitness.aggressiveFitness(game, playerStats, player, epoch)

    override fun crossover(candidatesFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> =
            Crossover.tournament(candidatesFitnessList)

    override fun selection(candidatesFitnessList: MutableList<UnitCandidate>,
                           childrenFitnessList: MutableList<UnitCandidate>): MutableList<UnitDecisionMaker> =
            Selection.selectBestPopulation(candidatesFitnessList, childrenFitnessList)

    override fun getTrainingAIs(epoch: Int): List<String> =
            if (epoch < ACTIVE_START) getPassiveAIS() else getActiveAIS()
}