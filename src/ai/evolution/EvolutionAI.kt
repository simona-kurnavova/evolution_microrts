package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.decisionMaker.AbstractAction
import ai.evolution.decisionMaker.State
import ai.evolution.strategyDecisionMaker.GlobalState
import rts.GameState
import rts.PlayerAction
import rts.units.UnitTypeTable
import java.util.ArrayList

class EvolutionAI(private val evaluate: (State, GlobalState) -> List<AbstractAction>,
                  private val unitTypeTable: UnitTypeTable? = null) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        val globalState = GlobalState(player, gs).apply { initialise() }

        for (unit in gs.units) {
            if (unit.player == player && gs.getActionAssignment(unit) == null) {
                val possibleUnitActions = unit.getUnitActions(gs)
                val state = State(player, gs, unit).apply { initialise() }

                val decisions = evaluate(state, globalState)
                decisions.forEach {
                    val unitAction = it.getUnitAction(state)

                    if (possibleUnitActions.contains(unitAction) && unit.canExecuteAction(unitAction, gs)
                            && gs.isUnitActionAllowed(unit, unitAction)) {
                        playerAction.addUnitAction(unit, unitAction)
                        return@forEach
                    }
                }
            }
        }
        return playerAction
    }

    override fun reset() {}

    override fun clone(): AI = EvolutionAI(evaluate)

    override fun getParameters(): MutableList<ParameterSpecification>  = ArrayList()
}