package ai.evolution.runners

import ai.core.AI
import ai.core.AIWithComputationBudget
import ai.evolution.*
import ai.evolution.utils.Utils.Companion.actions
import ai.evolution.utils.Utils.Companion.entities
import ai.evolution.state.AbstractAction
import ai.evolution.state.AbstractAction.Companion.types
import ai.evolution.state.State
import ai.evolution.utils.TrainingUtils
import ai.evolution.utils.TrainingUtils.BUDGET_INITIAL
import ai.evolution.utils.TrainingUtils.MODE
import ai.evolution.utils.TestingUtils.TESTING_BUDGET
import ai.evolution.utils.TestingUtils.TESTING_RUNS
import ai.evolution.utils.TrainingUtils.UTT_VERSION
import ai.evolution.gp.UnitDecisionMaker
import ai.evolution.neat.Genome
import ai.evolution.gpstrategy.GlobalState
import ai.evolution.utils.Utils
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.UnitAction.*
import rts.units.UnitTypeTable

class GameRunner(val gameSettings: GameSettings,
                 val fitness: (Game, ActionStatistics, Int) -> Pair<Double, Boolean>) {

    fun runGame(evaluate: (State, GlobalState) -> List<AbstractAction>, enemyAi: String, player: Int, budget: Int = BUDGET_INITIAL): Pair<Double, Boolean>? {
        val utt = UnitTypeTable(UTT_VERSION)

        val constructor = Class.forName(enemyAi).getConstructor(UnitTypeTable::class.java)
        val ai = (constructor.newInstance(utt) as AI)
        if (ai is AIWithComputationBudget) {
            ai.timeBudget = budget
        }

        return playGame(utt, player, EvolutionAI(evaluate, utt), ai)
    }

    fun runTournament(evaluate1: (State, GlobalState) -> List<AbstractAction>,
                      evaluate2: (State, GlobalState) -> List<AbstractAction>, player: Int = 0): Pair<Double, Boolean>? {
        val utt = UnitTypeTable(UTT_VERSION)
        return playGame(utt, player,  EvolutionAI(evaluate1, utt), EvolutionAI(evaluate2, utt))
    }

    fun runGameForAIs(evaluate: (State, GlobalState) -> List<AbstractAction>, ais: List<String>, print: Boolean = false,
                      budget: Int = TESTING_BUDGET, runsPerAi: Int = TESTING_RUNS): Pair<Double, Int> {
        var globalWins = 0
        var globalFitness = 0.0

        ais.forEach { ai ->
            var wins = 0
            var fitness = 0.0
            var bestFitness = -90000.0

            repeat(runsPerAi) {
                val fitnessEval = runGame(evaluate, ai, listOf(0, 1).random(), budget)
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
                println("Against AI: $ai")
                println(" - AVG fitness: ${fitness / runsPerAi}")
                println(" - Best fitness: $bestFitness")
                println(" - Wins: $wins / ${runsPerAi}")
            }
        }
        return Pair(globalFitness / (runsPerAi * ais.size), globalWins)
    }

    fun playTransparentGame(ai1: String, ai2: String, runs: Int) {
        val utt = UnitTypeTable(UTT_VERSION)

        var constructor = Class.forName(ai1).getConstructor(UnitTypeTable::class.java)
        val ai_1 = (constructor.newInstance(utt) as AI)
        if (ai_1 is AIWithComputationBudget) {
            ai_1.timeBudget = BUDGET_INITIAL
        }

        constructor = Class.forName(ai2).getConstructor(UnitTypeTable::class.java)
        val ai_2 = (constructor.newInstance(utt) as AI)
        if (ai_2 is AIWithComputationBudget) {
            ai_2.timeBudget = BUDGET_INITIAL
        }

        repeat(runs) {
            val game = getGame(utt, 0, ai_1, ai_2)
            try {
                game.start()
            } catch (e: Exception) {
                Utils.writeEverywhere("error: ${e.cause} from playTransparentGame()")
                Utils.writeEverywhere("error: ${e.printStackTrace()}")
            }
        }
    }

    private fun playGame(utt: UnitTypeTable, player: Int, ai1: AI, ai2: AI): Pair<Double, Boolean>?  {
        val game = getGame(utt, player, ai1, ai2)
        try {
            val fitness = fitness(game, game.start()[player], player)
            if (ai1 is EvolutionAI && MODE == TrainingUtils.Mode.TESTING) {
                //println("Average time of getAction() call is ${ai1.getAverageActionTime()} ns / " +
                //    "${ai1.getAverageActionTime() / 1000000} ms")
            }
            return fitness
        } catch (e: Exception) {
            Utils.writeEverywhere("error: ${e.cause} from playGame()")
            Utils.writeEverywhere("error: ${e.printStackTrace()}")
        }
        return null
    }

    private fun getGame(utt: UnitTypeTable, player: Int, ai1: AI, ai2: AI) = Game(utt, TrainingUtils.MAP_LOCATION, TrainingUtils.HEADLESS,
            TrainingUtils.PARTIALLY_OBSERVABLE, TrainingUtils.MAX_CYCLES, TrainingUtils.UPDATE_INTERVAL,
            if (player == 0) ai1 else ai2,
            if (player == 1) ai1 else ai2)

    companion object {

        fun getEvaluateLambda(unitDecisionMaker: UnitDecisionMaker) = { s: State, gs: GlobalState ->
            unitDecisionMaker.decide(s, gs).map { it.first.abstractAction }
        }

        fun getEvaluateLambda(neat: Genome) = { s: State, _: GlobalState ->
            decodeAction(neat.evaluateNetwork(s.getNeatInputs().toFloatArray()).toList())
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
                        entities[getHotOneIndex(decision.subList(offset, offset + entities.size))]
            }

            val unitTypes = UnitTypeTable(UTT_VERSION).unitTypes
            if (abstractAction.action == TYPE_PRODUCE) {
                // 7: what to produce
                offset += 2 + entities.size
                abstractAction.unitToProduce =
                        unitTypes[getHotOneIndex(decision.subList(offset, offset + unitTypes.size))].name
            }

            if (abstractAction.action == TYPE_ATTACK_LOCATION) {
                offset += 2 + entities.size + unitTypes.size
                abstractAction.entity = if(getHotOneIndex(decision.subList(offset, offset + 2)) == 0)
                    Utils.Companion.Entity.ENEMY
                else Utils.Companion.Entity.ENEMY_BASE
            }
            abstractAction.forceConsistency()
            return listOf(abstractAction)
        }

        private fun getHotOneIndex(list: List<Float>): Int = list.indexOf(list.maxByOrNull { it })
    }
}