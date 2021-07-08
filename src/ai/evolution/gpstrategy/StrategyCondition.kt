package ai.evolution.gpstrategy

import ai.evolution.utils.Utils.Companion.coinToss

class StrategyCondition {
    val partialGlobalState: PartialGlobalState = PartialGlobalState()
    val strategy: Strategy = Strategy() // equivalent of action

    fun evaluate(globalState: GlobalState) = globalState.compareTo(partialGlobalState)

    fun mutate() {
        if (coinToss()) partialGlobalState.mutate()
        else strategy.mutate()
    }
}