package ai.evolution.utils

import ai.evolution.operators.Fitness
import rts.ActionStatistics
import rts.Game
import java.io.File

object TrainingUtils {
    enum class TrainAI {
        GP, GP_STRATEGY, NEAT
    }

    enum class FitnessType {
        BASIC, AGGRESSIVE, PRODUCTIVE, TIME_INDEPENDENT
    }

    enum class Mode {
        TRAINING, TESTING, TOURNAMENT_TESTING
    }

    /**
     * Global settings. Applies to all of the models.
     */
    val MODE = Mode.TRAINING // Mode of the run
    val AI = TrainAI.NEAT // Model to train with
    val FITNESS = FitnessType.BASIC // Fitness for evaluation of the candidates

    const val RUNS = 1
    const val EPOCH_COUNT = 10 // Number of generations
    const val POPULATION = 100
    const val MAP_WIDTH = 16 // Size of the map

    /**
     * Genetic programming model settings only.
     */
    const val BEST_AI_EPOCH = 15000 // Minimum number of epochs, after which can training stop after finding the best candidate
    const val CONDITION_COUNT = 10 // Number of conditions per one unit
    const val COND_MUT_PROB = 0.14 // Probability of mutation
    const val PROB_BASE_ATTACK = 0.25 // Probability of base attack
    const val ACTIVE_START = 0 // In case of passive learning, otherwise 0

    /**
     * Neat settings only.
     */
    const val HIDDEN_UNITS = 10000 // Number of hidden units of networks

    /**
     * Adaptive budget settings.
     */
    const val ADAPTIVE_BUDGET = false
    const val TRESHOLD_FITNESS = 700 // Adapt when this fitness value is reached
    const val BUDGET_INITIAL = 10
    const val BUDGET_ADAPT_CONSTANT = 5 // Adapt fitness by adding this number
    const val BUDGET_EPOCH_STEP = 0 // Adapt fitness after this number of epochs
    const val BUDGET_UPPER_LIMIT = 100 // Stop when this fitness is reached and trained

    const val SCALE_BEST = 3 // Only for GP model, part of the population that have to reach given fitness

    /**
     * When loading population from file.
     * True if using population from file as initial population.
     */
    const val LOAD_FROM_FILE = false
    val LOAD_POPULATION_FILE = File("replace this")

    /**
     * Testing during training for better accuracy.
     */
    const val TESTING_WHILE_TRAINING = false
    const val TESTING_INTERVAL = 10 // every X epochs

    /**
     * Returns desired fitness function based on [FITNESS] settings for all the models (and testing).
     */
    fun getFitness(game: Game, playerStats: ActionStatistics, player: Int, epoch: Int?) = when(FITNESS) {
        FitnessType.BASIC -> Fitness.basicFitness(game, playerStats, player, epoch)
        FitnessType.AGGRESSIVE -> Fitness.aggressiveFitness(game, playerStats, player, epoch)
        FitnessType.PRODUCTIVE -> Fitness.productiveFitness(game, playerStats, player, epoch)
        FitnessType.TIME_INDEPENDENT -> Fitness.timeIndependentFitness(game, playerStats, player, epoch)
    }

    /**
     * Used during pre-training only, if [ACTIVE_START] is 0, it is not used at all.
     */
    fun getPassiveAIS(): List<String> = mutableListOf(
            "ai.RandomBiasedAI",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
    )

    /**
     * Returns AI opponents for model to train against.
     */
    fun getActiveAIS(): List<String> = mutableListOf(
            //"ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.mcts.naivemcts.NaiveMCTS",
            //"ai.mcts.naivemcts.NaiveMCTS",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            //"ai.RandomBiasedAI",
            //"ai.RandomBiasedAI",
    )

    fun printInfo(): String = "pop: $POPULATION, conditions: $CONDITION_COUNT, " +
            "epochs: $BEST_AI_EPOCH, mut: $COND_MUT_PROB, turns: $MAX_CYCLES, active_start: $ACTIVE_START"

    const val CORES_COUNT = 8 // number of processor cores/threads for parallelization
    const val HEADLESS = true
    const val PARTIALLY_OBSERVABLE = false
    const val MAX_CYCLES = 5000
    const val UPDATE_INTERVAL = 5 // ignored if headless == true
    const val SAVE_POPULATION_INTERVAL = 10
    const val UTT_VERSION = 2
    const val MAP_LOCATION = "data/maps/${MAP_WIDTH}x${MAP_WIDTH}/basesWorkers${MAP_WIDTH}x${MAP_WIDTH}.xml"
    const val BEST_LIST_SIZE = 10
}