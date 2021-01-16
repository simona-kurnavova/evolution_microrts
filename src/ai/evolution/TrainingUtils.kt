package ai.evolution

import ai.PassiveAI
import ai.RandomBiasedAI
import ai.core.AI
import ai.evolution.condition.DecisionMaker
import rts.GameSettings
import rts.units.Unit
import rts.units.UnitTypeTable

object TrainingUtils {
    const val POPULATION = 12
    const val CONDITION_COUNT = 30 // number of conditions for one unit
    const val EPOCH_COUNT = 400 // number of generations

    const val COND_MUT_PROB = 0.18
    const val CANDIDATE_COUNT = 2 // number of selection candidates for tournament
    const val CORES_COUNT = 4 // number of processor cores for parallelization
    const val TOURNAMENT_START = 50 // number of epoch when to start game tournaments between candidates
    const val ACTIVE_START = 5
    const val PARENT_COUNT = 2 // number of parents child has

    fun getPassiveAIS(gameSettings: GameSettings): List<Pair<AI, Double>> = mutableListOf(
            Pair(PassiveAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(PassiveAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0))

    fun getActiveAIS(gameSettings: GameSettings): List<Pair<AI, Double>> = mutableListOf(
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0))

    data class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double)
    data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val hp: Int, val hpBase: Int)
}