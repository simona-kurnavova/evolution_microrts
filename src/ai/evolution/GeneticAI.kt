package ai.evolution

import ai.core.AI
import ai.core.ParameterSpecification
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.DecisionMaker
import ai.evolution.condition.state.State
import rts.GameState
import rts.PlayerAction
import rts.UnitAction
import rts.UnitAction.DIRECTION_DOWN
import rts.UnitAction.TYPE_PRODUCE
import rts.units.UnitType
import rts.units.UnitTypeTable
import java.util.*

class GeneticAI(private val decisionMaker: DecisionMaker, private val unitTypeTable: UnitTypeTable? = null,
                private val printOptions: Boolean = false) : AI() {

    override fun getAction(player: Int, gs: GameState?): PlayerAction {
        val playerAction = PlayerAction()

        if (gs == null || !gs.canExecuteAnyAction(player) || gs.units.isNullOrEmpty())
            return playerAction

        for (unit in gs.units) {

            if (unit.type.name != "Base" && unit.type.name != "Worker" && unit.type.name != "Resource" && unit.type.name != "Barracks") {
               // if (unit.player == player)
               //     writeToFile("My unit: ${unit.type.name}")
                //else writeToFile("Enemy unit: ${unit.type.name}")
            }

            if (unit.player == player && gs.getActionAssignment(unit) == null) {

                val possibleUnitActions = unit.getUnitActions(gs)
                val state = State(player, gs, unit)
                state.initialise()

                if (printOptions)
                    writeToFile(decisionMaker.decide(state).toString(), actionFile)

                decisionMaker.decide(state).forEach {
                    val unitAction = it.first.getUnitAction(state)

                    /*if (it.first.abstractAction.action == TYPE_PRODUCE &&
                            possibleUnitActions.filter { it.type == TYPE_PRODUCE && it.unitType.cost > 1 }.isNotEmpty()) {
                        writeToFile(it.first.getUnitAction(state).toString(), actionFile)
                        writeToFile(possibleUnitActions.filter { it.type == TYPE_PRODUCE && it.unitType.cost > 1 }[0].toString(), actionFile)
                    }*/

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