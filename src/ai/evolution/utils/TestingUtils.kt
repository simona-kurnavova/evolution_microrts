package ai.evolution.utils

import java.io.File

/**
 * Utils for testing mode, given by [TrainingUtils.MODE].
 */
object TestingUtils {

    /**
     * Testing values. When [TrainingUtils.MODE] is [TrainingUtils.Mode.TESTING].
     */
    val TEST_FILE = File("build/libs/output/" +
            //"output/Tests/SGA/9. SGA - Auto adaptive budget 5/"+
            "NEAT_100_map=16_[ai.minimax.RTMiniMax.IDRTMinimax, ai.minimax.RTMiniMax.IDRTMinimax]_fit=BASIC_b=5_step=5_per=0e_hn=10000_e=30000_mut0.7_0.03_0.1_0.05_0.2_0.2"+
            //"/best_decision_maker")
            //"/tmp_best_genome")
            "/population_list")
    const val TESTING_RUNS = 100
    const val TESTING_BUDGET = 100

    const val TESTING_POP_RUNS = 100
    const val TESTING_POP_BUDGET = 100

    /**
     * Tournament values. When [TrainingUtils.MODE] is [TrainingUtils.Mode.TOURNAMENT_TESTING].
     */
    val AI_1_TYPE = TrainingUtils.TrainAI.GP
    val AI_1 = File("output/Tests/SGA/9. SGA - Auto adaptive budget 5/" +
            "SIMPLE_10_map=16_[IDRTMinimax, NaiveMCTS, InformedNaiveMCTS, RandomBiasedAI]_fit=BASIC_b=5_step=5_per=0e_cond=10_mut=0.14_as=0_em=800_e=25000" +
            "/best_decision_maker")
    val AI_2_TYPE = TrainingUtils.TrainAI.GP_STRATEGY
    val AI_2 = File("output/Tests/SGA Strategy/Auto-adaptive budget/" +
            "SIMPLE_STRATEGY_10_map=16_[IDRTMinimax, NaiveMCTS, InformedNaiveMCTS, RandomBiasedAI]_fit=BASIC_b=5_step=5_per=0e_cond=10_mut=0.14_10"+
            "/best_decision_maker")

    fun getTestingAIs(): MutableList<String> = mutableListOf(
            //"ai.RandomAI",
            "ai.RandomBiasedAI",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            //"ai.minimax.RTMiniMax.IDRTMinimax",
            //"ai.mcts.naivemcts.NaiveMCTS",
    )

    fun getFastTestingAIs(): MutableList<String> = mutableListOf(
            "ai.RandomAI",
            "ai.RandomBiasedAI",
            "ai.minimax.RTMiniMax.IDRTMinimax",
            "ai.mcts.informedmcts.InformedNaiveMCTS",
            "ai.mcts.naivemcts.NaiveMCTS",
    )
}