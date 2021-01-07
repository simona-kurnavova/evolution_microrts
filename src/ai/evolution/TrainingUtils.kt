package ai.evolution

import ai.evolution.condition.DecisionMaker
import rts.units.Unit

object TrainingUtils {
    const val POPULATION = 32 // must be dividable by 2
    const val CONDITION_COUNT = 40 // number of conditions for one unit
    const val EPOCH_COUNT = 400 // number of generations

    const val COND_MUT_PROB = 0.18
    const val CANDIDATE_COUNT = 2 // number of selection candidates for tournament
    const val CORES_COUNT = 8 // number of processor cores for parallelization
    const val TOURNAMENT_START = 50 // number of epoch when to start game tournaments between candidates
    const val ACTIVE_START = 15
    const val PARENT_COUNT = 2 // number of parents child has

    data class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double)
    data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val resourceCount: Int, val hp: Int,
                                var lost: Boolean = false)
}