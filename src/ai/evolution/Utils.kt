package ai.evolution

import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.ACTIVE_START
import ai.evolution.decisionMaker.TrainingUtils.AI
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_ADAPT_CONSTANT
import ai.evolution.decisionMaker.TrainingUtils.BEST_AI_EPOCH
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_INITIAL
import ai.evolution.decisionMaker.TrainingUtils.CONDITION_COUNT
import ai.evolution.decisionMaker.TrainingUtils.COND_MUT_PROB
import ai.evolution.decisionMaker.TrainingUtils.BUDGET_EPOCH_STEP
import ai.evolution.decisionMaker.TrainingUtils.EPOCH_COUNT
import ai.evolution.decisionMaker.TrainingUtils.FITNESS
import ai.evolution.decisionMaker.TrainingUtils.LOAD_FROM_FILE
import ai.evolution.decisionMaker.TrainingUtils.POPULATION
import ai.evolution.decisionMaker.TrainingUtils.MAP_WIDTH
import ai.evolution.decisionMaker.TrainingUtils.MAX_CYCLES
import ai.evolution.decisionMaker.TrainingUtils.RUNS
import ai.evolution.decisionMaker.TrainingUtils.getActiveAIS
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.neat.NEAT_Config.HIDDEN_NODES
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import ai.evolution.strategyDecisionMaker.StrategyTrainingUtils
import rts.GameState
import rts.UnitAction.*
import rts.units.Unit
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

class Utils {
    companion object {

        class UnitCandidate(var unitDecisionMaker: UnitDecisionMaker,
                            var fitness: Double, var wins: Int)

        data class StrategyCandidate(var strategyDecisionMaker: StrategyDecisionMaker,
                                     var fitness: Double, var wins: Int)

        data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val hp: Int, val hpBase: Int)

        /**
         * Root folder for this run output files.
         */
        val ROOT_OUTPUT_FOLDER =
                "output/${AI.name}_${POPULATION}_map=${MAP_WIDTH}_runs=${RUNS}${getActiveAIS()}_b=${BUDGET_INITIAL}" +
                        if (BUDGET_ADAPT_CONSTANT != 0) "_${BUDGET_ADAPT_CONSTANT}_step=${BUDGET_EPOCH_STEP}" else "" +
                        if (AI == TrainingUtils.TrainAI.NEAT)"_hn=${HIDDEN_NODES}_e=${EPOCH_COUNT}"
                        else "_cond=${CONDITION_COUNT}_mut=${COND_MUT_PROB}_as=${ACTIVE_START}_em=${BEST_AI_EPOCH}_e=${EPOCH_COUNT}" +
                        if (AI == TrainingUtils.TrainAI.SIMPLE_STRATEGY) "_${StrategyTrainingUtils.CONDITION_COUNT}" else "" +
                        if (LOAD_FROM_FILE) "from_file" else "" + "_fit=${FITNESS.name}"

        /**
         * Progress of fitness and victories throughout the training.
         */
        val mainFile = File("$ROOT_OUTPUT_FOLDER/epochs")

        /**
         * Json of best decision maker.
         */
        val conditionsFile = File("$ROOT_OUTPUT_FOLDER/best_decision_maker")

        /**
         * Final evaluation/testing result of best decision maker.
         */
        val evalFile = File("$ROOT_OUTPUT_FOLDER/evaluation")

        val averageBestFile = File("$ROOT_OUTPUT_FOLDER/average_best_fitness")

        val actionFile = File("$ROOT_OUTPUT_FOLDER/actions")

        val bestListFile = File("$ROOT_OUTPUT_FOLDER/list_best_decision_makers")

        val popListFile = File("$ROOT_OUTPUT_FOLDER/population_list")

        val directions = listOf(
            DIRECTION_NONE, DIRECTION_RIGHT, DIRECTION_LEFT, DIRECTION_UP, DIRECTION_DOWN
        )

        val directionsWithoutNone = listOf(
            DIRECTION_RIGHT, DIRECTION_LEFT, DIRECTION_UP, DIRECTION_DOWN
        )

        val actions = listOf(
            TYPE_NONE, TYPE_MOVE, TYPE_RETURN, TYPE_PRODUCE, TYPE_ATTACK_LOCATION, TYPE_HARVEST
        )

        enum class Keys {
            HAVE_RESOURCES, CARRY_RESOURCES,
            RESOURCE_CLOSE, RESOURCE_REACHABLE,
            BASE_CLOSE, BASE_REACHABLE,
            EMPTY_AROUND, SAFE_AROUND,
            AM_STRONG, ENEMY_CLOSE, ENEMY_REACHABLE, SURROUNDED,
            ENEMY_BASE_REACHABLE, ENEMY_BASE_CLOSE,
            WALL_AROUND, IN_CORNER, FRIEND_CLOSE, FRIEND_REACHABLE,
            OVERPOWERED,
            HAVE_BARRACKS, ENEMY_HAVE_BARRACKS, ENEMY_BARRACKS_CLOSE, ENEMY_BARRACKS_REACHABLE
        }

        val keys = listOf(
                Keys.HAVE_RESOURCES, Keys.CARRY_RESOURCES,
                Keys.RESOURCE_CLOSE, Keys.RESOURCE_REACHABLE,
                Keys.BASE_CLOSE, Keys.BASE_REACHABLE,
                Keys.EMPTY_AROUND, Keys.SAFE_AROUND,
                Keys.AM_STRONG, Keys.ENEMY_CLOSE, Keys.ENEMY_REACHABLE, Keys.SURROUNDED,
                Keys.ENEMY_BASE_REACHABLE, Keys.ENEMY_BASE_CLOSE,
                Keys.WALL_AROUND, Keys.IN_CORNER, Keys.FRIEND_CLOSE, Keys.FRIEND_REACHABLE,
                Keys.OVERPOWERED,
                Keys.HAVE_BARRACKS, Keys.ENEMY_HAVE_BARRACKS, Keys.ENEMY_BARRACKS_CLOSE, Keys.ENEMY_BARRACKS_REACHABLE
        )

        enum class Entity {
            NONE, WALL, ENEMY, FRIEND, RESOURCE, ME, ENEMY_BASE, MY_BASE
        }

        val entities = listOf(
            Entity.NONE, Entity.WALL, Entity.ENEMY, Entity.FRIEND, Entity.RESOURCE, Entity.ENEMY_BASE,
            Entity.MY_BASE, Entity.ME
        )

        val entitiesWithoutMe = listOf(
            Entity.NONE, Entity.WALL, Entity.ENEMY, Entity.FRIEND, Entity.RESOURCE, Entity.ENEMY_BASE,
            Entity.MY_BASE
        )

        fun coinToss(prob: Double = 0.5): Boolean = Random.nextDouble(0.0, 1.0) < prob

        fun getPositionIndex(position: Pair<Int, Int>, gs: GameState): Int =
            (position.second * gs.physicalGameState.width) + position.first

        fun writeToFile(text: String, file: File = mainFile) {
            if (file.exists()) {
                try {
                    Files.write(file.toPath(), "$text\n".toByteArray(), StandardOpenOption.APPEND)
                } catch (e: Exception) {
                    println(e)
                }
            } else {
                File(ROOT_OUTPUT_FOLDER).mkdirs()
                file.writeText("$text\n")
            }
        }

        fun writeEverywhere(text: String, file: File = mainFile) {
            writeToFile(text, file)
            println(text)
        }
    }
}