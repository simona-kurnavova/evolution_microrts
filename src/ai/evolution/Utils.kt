package ai.evolution

import rts.GameState
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.random.Random

class Utils {
    companion object {

        const val COND_MUT_PROB = 0.3

        val file = File("output")

        fun coinToss(prob: Double = 0.5): Boolean = Random.nextDouble(0.0, 1.0) < prob

        fun getPositionIndex(position: Pair<Int, Int>, gs: GameState): Int = (position.second * gs.physicalGameState.width) + position.first

        fun writeToFile(text: String) {
            if (file.exists()) {
                Files.write(file.toPath(), "$text\n".toByteArray(), StandardOpenOption.APPEND)
            } else {
                file.writeText("$text\n")
            }
        }
    }
}