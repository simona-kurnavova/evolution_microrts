package ai.evolution

import rts.GameState
import rts.UnitAction.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

class Utils {
    companion object {

        const val COND_MUT_PROB = 0.3

        const val PROB_BASE_ATTACK = 0.2

        const val PROB_STATE_GENERATE = 0.1

        const val PROB_STATE_MUTATE = 0.2

        const val WIDTH = 16

        val file = File("output")

        val directions = listOf(
            DIRECTION_NONE, DIRECTION_RIGHT, DIRECTION_LEFT, DIRECTION_UP, DIRECTION_DOWN
        )

        val directionsWithoutNone = listOf(
            DIRECTION_RIGHT, DIRECTION_LEFT, DIRECTION_UP, DIRECTION_DOWN
        )

        val actions = listOf(
            TYPE_NONE, TYPE_MOVE, TYPE_RETURN, TYPE_PRODUCE, TYPE_ATTACK_LOCATION, TYPE_HARVEST
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

        fun coinToss(prob: Double = 0.5): Boolean =
            Random.nextDouble(0.0, 1.0) < prob

        fun getPositionIndex(position: Pair<Int, Int>, gs: GameState): Int =
            (position.second * gs.physicalGameState.width) + position.first

        fun writeToFile(text: String) {
            if (file.exists()) {
                Files.write(file.toPath(), "$text\n".toByteArray(), StandardOpenOption.APPEND)
            } else {
                file.writeText("$text\n")
            }
        }
    }
}