package ai.evolution.decisionMaker

import java.io.File

object TrainingUtils {
    enum class TrainAI {
        SIMPLE, SIMPLE_STRATEGY, COMPLEX_STRATEGY, NEAT
    }

    /**
     * Global settings.
     */
    val AI = TrainAI.NEAT
    const val RUNS = 1
    const val EPOCH_COUNT = 3000 // number of generations
    const val POPULATION = 100
    const val MAP_WIDTH = 16
    const val TESTING_ONLY_MODE = false

    /**
     * Neat settings only.
     */
    const val HIDDEN_UNITS = 1000

    /**
     * SGA settings only.
     */
    const val BEST_AI_EPOCH = 500
    const val CONDITION_COUNT = 30 // number of conditions for one unit
    const val COND_MUT_PROB = 0.14
    const val PROB_BASE_ATTACK = 0.25
    const val ACTIVE_START = 0

    /**
     * Adaptive budget settings.
     */
    const val BUDGET_INITIAL = 100
    const val BUDGET_ADAPT_CONSTANT = 0
    const val BUDGET_EPOCH_STEP = 200

    val TEST_FILE = File("output/" +
            "20_SIMPLE_20_30_0.14_0.25_1650_2500_16x16_25_RUNS1[ai.minimax.RTMiniMax.IDRTMinimax, ai.minimax.RTMiniMax.IDRTMinimax]_40_10_jump200" +
            "/population_list")
    const val TESTING_RUNS = 100
    const val TESTING_BUDGET = 100

    const val CORES_COUNT = 8 // number of processor cores/threads for parallelization
    //const val TOURNAMENT_START = 50 // number of epoch when to start game tournaments between candidates
    const val PARENT_COUNT = 2 // number of parents child has
    const val ALLOW_WORKERS_ONLY = false
    const val SAVE_POPULATION_INTERVAL = 50
    const val UTT_VERSION = 2
    const val MAP_LOCATION = "data/maps/${MAP_WIDTH}x${MAP_WIDTH}/basesWorkers${MAP_WIDTH}x${MAP_WIDTH}.xml"

    const val HEADLESS = true
    const val PARTIALLY_OBSERVABLE = false
    const val MAX_CYCLES = 5000
    const val UPDATE_INTERVAL = 5 // ignored if headless == true

    const val TESTING_WHILE_TRAINING = false
    const val TESTING_INTERVAL = 10 // every X epochs

    const val BEST_LIST_SIZE = 5
    const val CANDIDATE_COUNT = 2 // number of selection candidates for tournament

    const val LOAD_FROM_FILE = true
    val LOAD_POPULATION_FILE = File(//"output/Tests/SGA/5. SGA - RTMinimax/" +
            "23_SIMPLE_5000_20_30_0.14_0_2050_3000_16_25_RUNS1[ai.minimax.RTMiniMax.IDRTMinimax, ai.minimax.RTMiniMax.IDRTMinimax]_10_10_jump200" +
            "/population_list")

    fun printInfo(): String = "pop: $POPULATION, conditions: $CONDITION_COUNT, " +
            "epochs: $BEST_AI_EPOCH, mut: $COND_MUT_PROB, turns: $MAX_CYCLES, active_start: $ACTIVE_START"

    fun getPassiveAIS(): List<String> = mutableListOf(
            //"ai.RandomBiasedAI",
            //"ai.mcts.informedmcts.InformedNaiveMCTS",
            //"ai.mcts.informedmcts.InformedNaiveMCTS"
            "ai.minimax.RTMiniMax.IDRTMinimax",
            "ai.minimax.RTMiniMax.IDRTMinimax",
    )

    fun getActiveAIS(): List<String> = mutableListOf(
            //"ai.abstraction.WorkerRush",
            //"ai.abstraction.WorkerRush",
            //"ai.abstraction.HeavyRush",
            //"ai.abstraction.HeavyRush",
            //"ai.portfolio.portfoliogreedysearch.UnitScriptsAI",
            //"ai.mcts.naivemcts.NaiveMCTS",
            //"ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.mcts.naivemcts.NaiveMCTS",
            //"ai.mcts.naivemcts.NaiveMCTS",
            //"ai.stochastic.UnitActionProbabilityDistributionAI",
            //"ai.mcts.informedmcts.InformedNaiveMCTS",
            //"ai.mcts.informedmcts.InformedNaiveMCTS",
            "ai.RandomBiasedAI",
            "ai.RandomBiasedAI",
            //"ai.RandomAI",
            //"ai.coac.CoacAI"
    )

    fun getTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomAI",
            "ai.RandomBiasedAI",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            "ai.mcts.naivemcts.NaiveMCTS",
            "ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.stochastic.UnitActionProbabilityDistributionAI",
            //"ai.portfolio.portfoliogreedysearch.UnitScriptsAI", = no such method (constructor) ???
            //"ai.minimax.RTMiniMax.RTMinimax",
            //"ai.minimax.ABCD.ABCD",
            //"ai.montecarlo.MonteCarlo",
            //"ai.mcts.mlps.MLPSMCTS", // slow
            //"ai.minimax.ABCD.IDABCD", // okayish
            //"ai.mcts.naivemcts.NaiveMCTS" // very slow
    )

    fun getFastTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomAI",
    )
}