package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.Utils.Companion.getPositionIndex
import rts.GameState
import rts.PlayerAction
import rts.UnitAction
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitTypeTable
import java.util.*

class GeneticProgrammingAI(private val decisionMaker: DecisionMaker, private val unitTypeTable: UnitTypeTable? = null) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        for (unit in gs.units) {

            if (unit.player == player && gs.getActionAssignment(unit) == null) {
                val possibleUnitActions = unit.getUnitActions(gs)
                val unitState = UnitState(player, gs, unit)
                val actions = decisionMaker.decide(unitState)
                val executable = mutableListOf<UnitAction>()

                for (i in actions) {
                    val action = i.getAction(unit)
                    if (possibleUnitActions.contains(action)) {
                        executable.add(action)
                    }
                }
                val action = if (executable.isNotEmpty()) executable.random() else null
                if (action != null) {
                    val position = calculateNextPosition(action, unit)

                    playerAction.addUnitAction(unit,
                            if (executable.isEmpty() || !unit.canExecuteAction(action, gs)
                                    || gs.resourceUsage.positionsUsed.contains(getPositionIndex(position, gs)))
                                UnitAction(TYPE_NONE)
                            else action
                    )
                }
            }
        }
        return playerAction
    }

    private fun calculateNextPosition(action: UnitAction?, unit: Unit): Pair<Int, Int> {
        var destinationX = unit.x
        var destinationY = unit.y

        if (action?.direction == DIRECTION_DOWN) destinationY += 1
        if (action?.direction == DIRECTION_UP) destinationY -= 1
        if (action?.direction == DIRECTION_RIGHT) destinationX += 1
        if (action?.direction == DIRECTION_LEFT) destinationX -= 1

        return Pair(destinationX, destinationY)
    }

    override fun clone(): AI = GeneticProgrammingAI(decisionMaker)

    override fun getParameters(): MutableList<ParameterSpecification> = ArrayList()

    override fun reset() {}
}