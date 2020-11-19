package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.Utils.Companion.getPositionIndex
import ai.evolution.condition.Condition
import ai.evolution.condition.DecisionMaker
import ai.evolution.condition.state.State
import rts.GameState
import rts.PlayerAction
import rts.UnitAction
import rts.UnitAction.*
import rts.units.Unit
import rts.units.UnitTypeTable
import java.util.*

class GeneticAI(private val decisionMaker: DecisionMaker, private val unitTypeTable: UnitTypeTable? = null) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        for (unit in gs.units) {

            if (unit.player == player && gs.getActionAssignment(unit) == null) {
                val possibleUnitActions = unit.getUnitActions(gs)
                val state = State(player, gs, unit)
                state.initialise()

                val actions = decisionMaker.decide(state)
                val executable = mutableListOf<Condition>()

                for (action in actions) {
                    if (possibleUnitActions.contains(action.first.getUnitAction(state))) {
                        executable.add(action.first)
                    }
                }

                executable.forEach {
                    if (unit.canExecuteAction(it.getUnitAction(state), gs)) {
                        playerAction.addUnitAction(unit, it.getUnitAction(state))
                        it.use()
                        return@forEach
                    }
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

    override fun clone(): AI = GeneticAI(decisionMaker)

    override fun getParameters(): MutableList<ParameterSpecification> = ArrayList()

    override fun reset() {}
}