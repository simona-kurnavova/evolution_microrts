package ai.evolution

import ai.evolution.TrainingUtils.ACTIVE_START
import ai.evolution.TrainingUtils.ALLOW_WORKERS_ONLY
import ai.evolution.TrainingUtils.BEST_AI_EPOCH
import ai.evolution.TrainingUtils.CANDIDATE_COUNT
import ai.evolution.TrainingUtils.CONDITION_COUNT
import ai.evolution.TrainingUtils.COND_MUT_PROB
import ai.evolution.TrainingUtils.EPOCH_COUNT
import ai.evolution.TrainingUtils.POPULATION
import ai.evolution.TrainingUtils.PROB_BASE_ATTACK
import ai.evolution.TrainingUtils.TOURNAMENT_START
import ai.evolution.TrainingUtils.MAP_WIDTH
import ai.evolution.TrainingUtils.UTT_VERSION
import ai.evolution.condition.DecisionMaker
import rts.GameState
import rts.UnitAction.*
import rts.units.Unit
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

class Utils {
    companion object {
        data class EvaluatedCandidate(var decisionMaker: DecisionMaker, var fitness: Double, var wins: Int)
        data class PlayerStatistics(val id: Int, val units: List<Unit?>?, val hp: Int, val hpBase: Int)

        /**
         * Root folder for this run output files.
         */
        const val ROOT_OUTPUT_FOLDER =
                "output/${POPULATION}_${CONDITION_COUNT}_${COND_MUT_PROB}_${PROB_BASE_ATTACK}" +
                        "_${CANDIDATE_COUNT}_${!ALLOW_WORKERS_ONLY}_${ACTIVE_START}" +
                        "_${BEST_AI_EPOCH}_${EPOCH_COUNT}_${TOURNAMENT_START}" +
                        "_${UTT_VERSION}_${MAP_WIDTH}x${MAP_WIDTH}"

        /**
         * Progress of fitness and victories throughout the training.
         */
        val mainFile = File("$ROOT_OUTPUT_FOLDER/epochs")

        /**
         * Json of best decision maker.
         */
        val conditionsFile = File("best_decision_maker")

        /**
         * Final evaluation/testing result of best decision maker.
         */
        val evalFile = File("$ROOT_OUTPUT_FOLDER/evaluation")

        val actionFile = File("$ROOT_OUTPUT_FOLDER/actions")

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
            ENEMY_CLOSE, RESOURCE_CLOSE, CARRY_RESOURCES, EMPTY_AROUND,
            SURROUNDED, ENEMY_BASE_CLOSE, FRIEND_CLOSE,
            OVERPOWERED
        }

        val keys = listOf(
            Keys.ENEMY_CLOSE, Keys.RESOURCE_CLOSE, Keys.CARRY_RESOURCES, Keys.EMPTY_AROUND, Keys.SURROUNDED, Keys.FRIEND_CLOSE,
            Keys.ENEMY_BASE_CLOSE, Keys.OVERPOWERED
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
    }
}