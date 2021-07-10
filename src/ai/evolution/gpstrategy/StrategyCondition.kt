package ai.evolution.gpstrategy

import ai.evolution.utils.Utils.Companion.coinToss

/**
 * Condition for GP model with strategy.
 */
class StrategyCondition {
    val partialGlobalState: PartialGlobalState = PartialGlobalState()
    val strategy: Strategy = Strategy() // adjusts priorities of actions evaluated by UnitDecisionMaker.

    fun evaluate(globalState: GlobalState) = globalState.compareTo(partialGlobalState)

    fun mutate() {
        if (coinToss()) partialGlobalState.mutate()
        else strategy.mutate()
    }
}