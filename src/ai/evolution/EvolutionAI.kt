package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.state.AbstractAction
import ai.evolution.state.State
import ai.evolution.gpstrategy.GlobalState
import rts.GameState
import rts.PlayerAction
import rts.units.UnitTypeTable
import java.util.ArrayList
import kotlin.system.measureNanoTime

/**
 * AI (real-time) used to run models of NEAT and genetic programming. It accepts [evaluate] method that decides next action
 * for the unit based on the current unit state and global state.
 */
class EvolutionAI(private val evaluate: (State, GlobalState) -> List<AbstractAction>,
                  private val unitTypeTable: UnitTypeTable? = null) : AI() {

    /**
     * Variables for time measurement.
     */
    private var globalTime: Long = 0
    private var actionCallCount: Long = 0

    /**
     * Returns list of actions assigned to the units.
     */
    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()
        globalTime += measureNanoTime {

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
        }
        actionCallCount ++
        return playerAction
    }

    /**
     * Time in nanoseconds.
     */
    fun getAverageActionTime() = globalTime.toDouble() / actionCallCount.toDouble()

    override fun reset() {}

    override fun clone(): AI = EvolutionAI(evaluate)

    override fun getParameters(): MutableList<ParameterSpecification>  = ArrayList()
}