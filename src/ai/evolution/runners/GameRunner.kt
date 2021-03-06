package ai.evolution.runners

import ai.evolution.*
import ai.evolution.Utils.Companion.actions
import ai.evolution.Utils.Companion.entitiesWithoutMe
import ai.evolution.decisionMaker.AbstractAction
import ai.evolution.decisionMaker.AbstractAction.Companion.types
import ai.evolution.decisionMaker.State
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.decisionMaker.TrainingUtils.UTT_VERSION
import ai.evolution.neat.Genome
import ai.evolution.strategyDecisionMaker.GlobalState
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.UnitAction.*
import rts.units.UnitTypeTable

class GameRunner(val gameSettings: GameSettings,
                 val fitness: (Game, ActionStatistics, Int) -> Pair<Double, Boolean>) {

    fun runGame(evaluate: (State, GlobalState) -> List<AbstractAction>, enemyAi: String, player: Int): Pair<Double, Boolean>? {
        val evolutionAI = EvolutionAI(evaluate, UnitTypeTable(gameSettings.uttVersion))
        val game = Game(UnitTypeTable(UTT_VERSION), TrainingUtils.MAP_LOCATION, TrainingUtils.HEADLESS, TrainingUtils.PARTIALLY_OBSERVABLE, TrainingUtils.MAX_CYCLES,
                TrainingUtils.UPDATE_INTERVAL, evolutionAI, enemyAi, player == 1)
        try {
            return fitness(game, game.start()[player], player)
        } catch (e: Exception) {
            Utils.writeToFile("error: ${e.message}")
        }
        return null
    }

    fun runGameForAIs(evaluate: (State, GlobalState) -> List<AbstractAction>, ais: List<String>, runsPerAi: Int = 1, print: Boolean = false): Pair<Double, Int> {
        var globalWins = 0
        var globalFitness = 0.0

        ais.forEach { ai ->
            var wins = 0
            var fitness = 0.0
            var bestFitness = -90000.0

            repeat(runsPerAi) {
                val fitnessEval = runGame(evaluate, ai, listOf(0, 1).random())
                if (fitnessEval != null) {
                    fitness += fitnessEval.first
                    wins += if (fitnessEval.second) 1 else 0
                    if (fitnessEval.first > bestFitness)
                        bestFitness = fitnessEval.first
                }
            }

            globalFitness += fitness
            globalWins += wins

            if (print) {
                Utils.writeToFile("Against AI: $ai", Utils.evalFile)
                Utils.writeToFile(" - AVG fitness: ${fitness / TrainingUtils.TESTING_RUNS}", Utils.evalFile)
                Utils.writeToFile(" - Best fitness: $bestFitness", Utils.evalFile)
                Utils.writeToFile(" - Wins: $wins / ${TrainingUtils.TESTING_RUNS}", Utils.evalFile)
            }
        }
        return Pair(globalFitness / (runsPerAi * ais.size), globalWins)
    }

    fun getEvaluateLambda(unitDecisionMaker: UnitDecisionMaker) = { s: State, gs: GlobalState ->
        unitDecisionMaker.decide(s, gs).map { it.first.abstractAction }
    }

    fun getEvaluateLambda(neat: Genome) = { s: State, _: GlobalState ->
        decodeAction(neat.evaluateNetwork(s.getInputs().toFloatArray()).toList())
    }

    private fun decodeAction(decision: List<Float>): List<AbstractAction> {
        var offset = 0
        // 6: simple action
        val abstractAction = AbstractAction()
        abstractAction.action = actions[getHotOneIndex(decision.subList(offset, offset + actions.size))]
        offset += actions.size

        if (abstractAction.action == TYPE_MOVE) {
            // 2: from/to
            abstractAction.type = types[getHotOneIndex(decision.subList(offset, offset + 2))]
            offset += 2

            // 7: entitiesWithoutMe
            abstractAction.entity =
                    entitiesWithoutMe[getHotOneIndex(decision.subList(offset, offset + entitiesWithoutMe.size))]
        }

        val unitTypes = UnitTypeTable(UTT_VERSION).unitTypes
        if (abstractAction.action == TYPE_PRODUCE) {
            // 7: what to produce
            offset += 2 + entitiesWithoutMe.size
            abstractAction.unitToProduce =
                    unitTypes[getHotOneIndex(decision.subList(offset, offset + unitTypes.size))].name
        }

        if (abstractAction.action == TYPE_ATTACK_LOCATION) {
            offset += 2 + entitiesWithoutMe.size + unitTypes.size
            abstractAction.entity = if(getHotOneIndex(decision.subList(offset, offset + 2)) == 0)
                Utils.Companion.Entity.ENEMY
            else Utils.Companion.Entity.ENEMY_BASE
        }
        abstractAction.forceConsistency()
        return listOf(abstractAction)
    }

    private fun getHotOneIndex(list: List<Float>): Int = list.indexOf(list.maxByOrNull { it })
}