package ai.evolution.utils

import java.io.File

/**
 * Utils for testing mode, given by [TrainingUtils.MODE].
 */
object TestingUtils {

    /**
     * Testing values. When [TrainingUtils.MODE] is [TrainingUtils.Mode.TESTING].
     */
    val TEST_FILE = File("output/GP_10_map=16_[ai.RandomBiasedAI, ai.RandomBiasedAI]_fit=BASIC_b=5_step=5_per=0e_cond=5_mut=0.14_as=0_em=500_e=200/population_list")

    /**
     * Test params.
     */
    const val TESTING_RUNS = 100
    const val TESTING_BUDGET = 100

    /**
     * Test params for testing [TEST_FILE] with more than one individual. The best is then tested normally.
     */
    const val TESTING_POP_RUNS = 100
    const val TESTING_POP_BUDGET = 100

    /**
     * Tournament values. When [TrainingUtils.MODE] is [TrainingUtils.Mode.TOURNAMENT_TESTING].
     */
    val AI_1_TYPE = TrainingUtils.TrainAI.GP
    val AI_1 = File("Location of GP model")
    val AI_2_TYPE = TrainingUtils.TrainAI.NEAT
    val AI_2 = File("Location of NEAT model")

    fun getTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomAI",
            "ai.RandomBiasedAI",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            "ai.minimax.RTMiniMax.IDRTMinimax",
            "ai.mcts.naivemcts.NaiveMCTS",
    )

    /**
     * Used for testing the whole generation (test file with more than 1 individual),
     * usually with lower number of runs. The best AI from these testing is then
     * tested using testing AIs.
     */
    fun getFastTestingAIs(): MutableList<String> = mutableListOf(
            //"ai.RandomAI",
            "ai.RandomBiasedAI",
            //"ai.mcts.naivemcts.NaiveMCTS",
            //"ai.mcts.informedmcts.InformedNaiveMCTS",
            //"ai.minimax.RTMiniMax.IDRTMinimax",
    )
}