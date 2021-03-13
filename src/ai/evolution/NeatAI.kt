package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.Utils.Companion.entities
import ai.evolution.Utils.Companion.entitiesWithoutMe
import ai.evolution.decisionMaker.AbstractAction
import ai.evolution.decisionMaker.AbstractAction.Companion.types
import ai.evolution.decisionMaker.State
import ai.evolution.neat.Genome
import rts.GameState
import rts.PlayerAction
import rts.UnitAction.*
import rts.units.UnitTypeTable
import java.util.*
import kotlin.math.abs
import kotlin.random.Random

class NeatAI(private val neat: Genome, private val unitTypeTable: UnitTypeTable? = null) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        for (unit in gs.units) {
            if (unit.player == player && gs.getActionAssignment(unit) == null) {

                val possibleUnitActions = unit.getUnitActions(gs)
                val state = State(player, gs, unit).apply { initialise() }
                val decision = neat.evaluateNetwork(state.getInputs().toFloatArray()).toList()

                val unitAction = decodeAction(decision).getUnitAction(state)
                if (possibleUnitActions.contains(unitAction) && unit.canExecuteAction(unitAction, gs)
                        && gs.isUnitActionAllowed(unit, unitAction)) {
                    playerAction.addUnitAction(unit, unitAction)
                }
            }
        }
        return playerAction
    }

    private fun decodeAction(decision: List<Float>): AbstractAction {
        val abstractAction = AbstractAction()
        abstractAction.action = when(decision.indexOf(decision.maxByOrNull { it })) {
            0 -> TYPE_HARVEST
            1 -> TYPE_RETURN
            2 -> TYPE_ATTACK_LOCATION
            3 -> TYPE_MOVE
            4 -> TYPE_PRODUCE
            else -> TYPE_NONE
        }
        abstractAction.onActionChangeSetup()
        return abstractAction
    }

    override fun clone(): AI = NeatAI(neat)

    override fun getParameters(): MutableList<ParameterSpecification> = ArrayList()

    override fun reset() {}
}