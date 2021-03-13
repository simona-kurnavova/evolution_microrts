package ai.evolution.decisionMaker

object TrainingUtils {
    enum class TrainAI {
        SIMPLE, SIMPLE_STRATEGY, COMPLEX_STRATEGY, NEAT
    }

    val AI = TrainAI.SIMPLE
    const val RUNS = 5

    const val POPULATION = 24
    const val CONDITION_COUNT = 35 // number of conditions for one unit
    const val EPOCH_COUNT = 6000 // number of generations

    const val COND_MUT_PROB = 0.18
    const val PROB_BASE_ATTACK = 0.25

    const val CANDIDATE_COUNT = 3 // number of selection candidates for tournament
    const val CORES_COUNT = 8 // number of processor cores/threads for parallelization
    const val TOURNAMENT_START = 50 // number of epoch when to start game tournaments between candidates
    const val ACTIVE_START = 0
    const val PARENT_COUNT = 2 // number of parents child has

    const val ALLOW_WORKERS_ONLY = false
    const val BEST_AI_EPOCH = 500
    const val TESTING_RUNS = 8
    const val SLOW_TESTING = false

    const val UTT_VERSION = 2
    const val MAP_WIDTH = 16
    const val MAP_LOCATION = "data/maps/${MAP_WIDTH}x${MAP_WIDTH}/basesWorkers${MAP_WIDTH}x${MAP_WIDTH}.xml"

    const val HEADLESS = true
    const val PARTIALLY_OBSERVABLE = false
    const val MAX_CYCLES = 5000
    const val UPDATE_INTERVAL = 5 // ignored if headless == true

    fun printInfo(): String = "${AI.name} pop: $POPULATION, conditions: $CONDITION_COUNT, epochs: $BEST_AI_EPOCH"

    fun getPassiveAIS(): List<String> = mutableListOf(
            "ai.PassiveAI",
            "ai.PassiveAI",
            "ai.RandomBiasedAI"
    )

    fun getActiveAIS(): List<String> = mutableListOf(
            "ai.RandomBiasedAI",
            "ai.RandomBiasedAI",
            "ai.RandomBiasedAI"
    )

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