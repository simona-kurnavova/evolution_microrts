package ai.evolution.utils

import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.neat.NEAT_Config.*
import ai.evolution.gpstrategy.StrategyDecisionMaker
import rts.UnitAction
import rts.units.Unit
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

/**
 * Helping utility functions for the evaluation and model training.
 */
class Utils {
    companion object {
        /**
         * Root folder for this run output files.
         */
        val ROOT_OUTPUT_FOLDER =
                "output/${TrainingUtils.AI.name}_${TrainingUtils.POPULATION}_map=${TrainingUtils.MAP_WIDTH}_${TrainingUtils.getActiveAIS()}" +
                        "_fit=${TrainingUtils.FITNESS.name}_b=${TrainingUtils.BUDGET_INITIAL}" +
                        (if (TrainingUtils.BUDGET_ADAPT_CONSTANT != 0) "_step=${TrainingUtils.BUDGET_ADAPT_CONSTANT}_per=${TrainingUtils.BUDGET_EPOCH_STEP}e" else "") +
                        if (TrainingUtils.AI == TrainingUtils.TrainAI.NEAT)
                            "_hn=${HIDDEN_NODES}_e=${TrainingUtils.EPOCH_COUNT}_mut${WEIGHT_MUTATION_CHANCE}" +
                                    "_${NODE_MUTATION_CHANCE}_${CONNECTION_MUTATION_CHANCE}_${BIAS_CONNECTION_MUTATION_CHANCE}_${DISABLE_MUTATION_CHANCE}_${ENABLE_MUTATION_CHANCE}"
                        else "_cond=${TrainingUtils.CONDITION_COUNT}_mut=${TrainingUtils.COND_MUT_PROB}_as=${TrainingUtils.ACTIVE_START}_em=${TrainingUtils.BEST_AI_EPOCH}_e=${TrainingUtils.EPOCH_COUNT}" +
                        if (TrainingUtils.AI == TrainingUtils.TrainAI.GP_STRATEGY) "_${StrategyTrainingUtils.CONDITION_COUNT}" else "" +
                        if (TrainingUtils.LOAD_FROM_FILE) "from_file" else ""

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

        @JvmStatic
        val dataFile = File("$ROOT_OUTPUT_FOLDER/DATA_${TrainingUtils.getActiveAIS()}_${TestingUtils.getTestingAIs()}")

        val directions = listOf(
                UnitAction.DIRECTION_NONE, UnitAction.DIRECTION_RIGHT, UnitAction.DIRECTION_LEFT, UnitAction.DIRECTION_UP, UnitAction.DIRECTION_DOWN
        )

        val directionsWithoutNone = listOf(
                UnitAction.DIRECTION_RIGHT, UnitAction.DIRECTION_LEFT, UnitAction.DIRECTION_UP, UnitAction.DIRECTION_DOWN
        )

        val actions = listOf(
                UnitAction.TYPE_NONE, UnitAction.TYPE_MOVE, UnitAction.TYPE_RETURN, UnitAction.TYPE_PRODUCE, UnitAction.TYPE_ATTACK_LOCATION, UnitAction.TYPE_HARVEST
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
            HAVE_BARRACKS, ENEMY_HAVE_BARRACKS, ENEMY_BARRACKS_CLOSE, ENEMY_BARRACKS_REACHABLE,
            BARRACKS_CLOSE, BARRACKS_REACHABLE
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
                Keys.HAVE_BARRACKS, Keys.ENEMY_HAVE_BARRACKS,
                Keys.ENEMY_BARRACKS_CLOSE, Keys.ENEMY_BARRACKS_REACHABLE,
                Keys.BARRACKS_CLOSE, Keys.BARRACKS_REACHABLE
        )

        enum class Entity {
            NONE, WALL, ENEMY, FRIEND, RESOURCE, ME, ENEMY_BASE, MY_BASE
        }

        val entities = listOf(
            Entity.NONE, Entity.WALL, Entity.ENEMY, Entity.FRIEND, Entity.RESOURCE, Entity.ENEMY_BASE,
            Entity.MY_BASE
        )

        fun coinToss(prob: Double = 0.5): Boolean = Random.nextDouble(0.0, 1.0) < prob

        @JvmStatic
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

        @JvmStatic
        fun writeEverywhere(text: String, file: File = mainFile) {
            writeToFile(text, file)
            println(text)
        }

        class UnitCandidate(var unitDecisionMaker: UnitDecisionMaker,
                            var fitness: Double, var wins: Int)

        data class StrategyCandidate(var strategyDecisionMaker: StrategyDecisionMaker,
                                     var fitness: Double, var wins: Int)

        data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val hp: Int, val hpBase: Int)
    }

}