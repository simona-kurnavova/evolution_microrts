package ai.evolution.runners

import ai.evolution.*
import ai.evolution.decisionMaker.AbstractAction
import ai.evolution.decisionMaker.State
import ai.evolution.decisionMaker.UnitDecisionMaker
import ai.evolution.decisionMaker.TrainingUtils
import ai.evolution.neat.Genome
import ai.evolution.strategyDecisionMaker.GlobalState
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.UnitAction
import rts.units.UnitTypeTable

class GameRunner(val gameSettings: GameSettings,
                 val fitness: (Game, ActionStatistics, Int) -> Pair<Double, Boolean>) {

    fun runGame(evaluate: (State, GlobalState) -> List<AbstractAction>, enemyAi: String, player: Int): Pair<Double, Boolean>? {
        val evolutionAI = EvolutionAI(evaluate, UnitTypeTable(gameSettings.uttVersion))
        val game = Game(UnitTypeTable(TrainingUtils.UTT_VERSION), TrainingUtils.MAP_LOCATION, TrainingUtils.HEADLESS, TrainingUtils.PARTIALLY_OBSERVABLE, TrainingUtils.MAX_CYCLES,
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
        val abstractAction = AbstractAction()
        abstractAction.action = when(decision.indexOf(decision.maxByOrNull { it })) {
            0 -> UnitAction.TYPE_HARVEST
            1 -> UnitAction.TYPE_RETURN
            2 -> UnitAction.TYPE_ATTACK_LOCATION
            3 -> UnitAction.TYPE_MOVE
            4 -> UnitAction.TYPE_PRODUCE
            else -> UnitAction.TYPE_NONE
        }
        abstractAction.onActionChangeSetup()
        return listOf(abstractAction)
    }
}