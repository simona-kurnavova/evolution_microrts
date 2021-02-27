package ai.evolution.decisionMaker

import ai.PassiveAI
import ai.RandomBiasedAI
import ai.core.AI
import com.google.gson.Gson
import rts.GameSettings
import rts.units.UnitTypeTable

object TrainingUtils {
    const val POPULATION = 128
    const val CONDITION_COUNT = 40 // number of conditions for one unit
    const val EPOCH_COUNT = 400 // number of generations

    const val COND_MUT_PROB = 0.18
    const val PROB_BASE_ATTACK = 0.25

    const val CANDIDATE_COUNT = 3 // number of selection candidates for tournament
    const val CORES_COUNT = 8 // number of processor cores/threads for parallelization
    const val TOURNAMENT_START = 50 // number of epoch when to start game tournaments between candidates
    const val ACTIVE_START = 0
    const val PARENT_COUNT = 2 // number of parents child has

    const val ALLOW_WORKERS_ONLY = false
    const val BEST_AI_EPOCH = 100
    const val TESTING_RUNS = 8
    const val SLOW_TESTING = false

    const val UTT_VERSION = 2
    const val MAP_WIDTH = 16
    const val MAP_LOCATION = "maps/${MAP_WIDTH}x${MAP_WIDTH}/basesWorkers${MAP_WIDTH}x${MAP_WIDTH}.xml"

    const val HEADLESS = true
    const val PARTIALLY_OBSERVABLE = false
    const val MAX_CYCLES = 5000
    const val UPDATE_INTERVAL = 5 // ignored if headless == true

    const val STRATEGY_AI = false
    const val RUNS = 10

    fun printInfo(): String = Gson().toJson(this)

    fun getPassiveAIS(gameSettings: GameSettings): List<Pair<AI, Double>> = mutableListOf(
            Pair(PassiveAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(PassiveAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0))

    fun getActiveAIS(gameSettings: GameSettings): List<Pair<AI, Double>> = mutableListOf(
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0),
            Pair(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)), 1.0))

    fun getFastTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomBiasedAI",
            "ai.RandomBiasedSingleUnitAI",
            "ai.RandomAI",
            "ai.stochastic.UnitActionProbabilityDistributionAI",
            //"ai.portfolio.portfoliogreedysearch.UnitScriptsAI", = no such method (constructor) ???
            //"ai.minimax.RTMiniMax.RTMinimax",
            //"ai.minimax.ABCD.ABCD"
    )

    fun getSlowTestingAIs(): MutableList<String> = mutableListOf(
            //"ai.montecarlo.MonteCarlo",
            "ai.mcts.mlps.MLPSMCTS", // slow
            "ai.mcts.informedmcts.InformedNaiveMCTS", // relatively fast
            "ai.minimax.ABCD.IDABCD", // okayish
            "ai.minimax.RTMiniMax.IDRTMinimax",
            "ai.mcts.naivemcts.NaiveMCTS" // very slow
    )
}