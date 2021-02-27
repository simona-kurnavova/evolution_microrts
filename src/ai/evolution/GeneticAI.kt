package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.decisionMaker.DecisionMaker
import ai.evolution.decisionMaker.State
import ai.evolution.decisionMaker.TrainingUtils.STRATEGY_AI
import ai.evolution.strategyDecisionMaker.GlobalState
import ai.evolution.strategyDecisionMaker.StrategyDecisionMaker
import rts.GameState
import rts.PlayerAction
import rts.units.UnitTypeTable
import java.util.*

class GeneticAI(private val decisionMaker: DecisionMaker, private val unitTypeTable: UnitTypeTable? = null) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        val globalState = GlobalState(player, gs).apply { initialise() }

        for (unit in gs.units) {
            if (unit.player == player && gs.getActionAssignment(unit) == null) {

                val possibleUnitActions = unit.getUnitActions(gs)
                val state = State(player, gs, unit).apply { initialise() }

                val decisions = if(STRATEGY_AI) decisionMaker.decide(state)
                else decisionMaker.decide(state, globalState)

                decisions.forEach {
                    val unitAction = it.first.getUnitAction(state)

                    if (possibleUnitActions.contains(unitAction) && unit.canExecuteAction(unitAction, gs)
                            && gs.isUnitActionAllowed(unit, unitAction)) {
                        playerAction.addUnitAction(unit, unitAction)
                        it.first.use()
                        return@forEach
                    }
                }
            }
        }
        return playerAction
    }

    override fun clone(): AI = GeneticAI(decisionMaker)

    override fun getParameters(): MutableList<ParameterSpecification> = ArrayList()

    override fun reset() {}
}