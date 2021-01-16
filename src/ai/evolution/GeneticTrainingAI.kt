package ai.evolution

import ai.evolution.TrainingUtils.ACTIVE_START
import ai.evolution.condition.DecisionMaker
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.PhysicalGameState
import java.util.stream.Collectors
import kotlin.random.Random
import ai.evolution.TrainingUtils.EvaluatedCandidate
import ai.evolution.TrainingUtils.PlayerStatistics
import ai.evolution.TrainingUtils.CANDIDATE_COUNT
import ai.evolution.TrainingUtils.CONDITION_COUNT
import ai.evolution.TrainingUtils.CORES_COUNT
import ai.evolution.TrainingUtils.EPOCH_COUNT
import ai.evolution.TrainingUtils.PARENT_COUNT
import ai.evolution.TrainingUtils.POPULATION
import ai.evolution.TrainingUtils.TOURNAMENT_START
import ai.evolution.TrainingUtils.getActiveAIS
import ai.evolution.TrainingUtils.getPassiveAIS
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.writeToFile
import java.lang.Exception
import kotlin.math.abs

class GeneticTrainingAI {
    
    private var candidates = mutableListOf<DecisionMaker>()
    private var candidatesFitnessList = mutableListOf<EvaluatedCandidate>()
    private var childrenFitnessList = mutableListOf<EvaluatedCandidate>()
    private var bestCandidate: EvaluatedCandidate? = null

    fun train(gameSettings: GameSettings) {
        initialisePopulation()

        for (epoch in 0 until EPOCH_COUNT) {
            writeToFile("Epoch $epoch")
            candidatesFitnessList = evaluateFitness(candidates, gameSettings, epoch) // Evaluate fitness of candidates
            printFitnessStats()

            val children = crossover() // Crossover between candidates
            children.forEach { it.mutate() } // Mutate children
            childrenFitnessList = evaluateFitness(children, gameSettings, epoch) // Evaluate children
            printBestCandidate(epoch)

            selectNewPopulation() // Selection
        }
    }

    /**
     * Initialise random population of [POPULATION] size with [CONDITION_COUNT] number of conditions for each unit.
     */
    private fun initialisePopulation() {
        repeat(POPULATION) { candidates.add(DecisionMaker(CONDITION_COUNT)) }
    }

    private fun evaluateFitness(candidates: MutableList<DecisionMaker>, gameSettings: GameSettings, epoch: Int): MutableList<EvaluatedCandidate> {
        val evaluatedCandidates = mutableListOf<EvaluatedCandidate>()

        val AIs = if (epoch < ACTIVE_START) getPassiveAIS(gameSettings) else getActiveAIS(gameSettings)

        /*val bestDecisionMaker = bestCandidate?.decisionMaker
        if (bestDecisionMaker != null && epoch >= TOURNAMENT_START) {
            AIs.add(GeneticAI(bestDecisionMaker))
        }*/

        candidates.chunked(CORES_COUNT).forEach { list ->
            var index = Random.nextInt()
            list.parallelStream().map {
                var fitness = 0.0
                AIs.forEach { ai ->
                    val player = 1//listOf(0, 1).random()
                    val game = if (player == 0) {
                        Game(gameSettings, GeneticAI(it, null), ai.first)
                    } else Game(gameSettings, ai.first, GeneticAI(it, null))

                    //try {
                        val actionStatistics = game.start()
                        fitness += (calculateFitness(game, actionStatistics[player], it.getCountUsedConditions(), player, epoch)).toDouble()
                    //} catch (e: Exception) {
                    //    writeToFile("error: ${e.message}")
                    //}
                }
                fitness /= AIs.size.toDouble() // average fitness
                evaluatedCandidates.add(EvaluatedCandidate(it, fitness))
            }.collect(Collectors.toMap({ ++index + Random.nextInt() }) { p: Any -> p })
        }

        evaluatedCandidates.sortByDescending { it.fitness }
        if (bestCandidate == null || bestCandidate!!.fitness <= evaluatedCandidates[0].fitness) {
            bestCandidate = evaluatedCandidates[0]
        }
        return evaluatedCandidates
    }

    private fun calculateFitness(game: Game, playerStats: ActionStatistics, countUsedConditions: Double, player: Int = 1, epoch: Int): Double {
        val stats = getStats(game.gs.physicalGameState)
        val hp = stats[player].hp.toDouble() - stats[abs(player - 1)].hp

        val hpBase = stats[player].hpBase.toDouble() - stats[abs(player - 1)].hpBase

        var points = if (epoch < ACTIVE_START) playerStats.produced.toDouble()
            else ((playerStats.damageDone.toDouble() + playerStats.produced + 1) - (playerStats.enemyDamage + 1)) + (hp * 2.0) + (hpBase * 5)

        if (epoch >= ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 1000000 / game.gs.time
                writeToFile("WIN = $points, time=${game.gs.time}")
            }

        writeToFile("points=$points, time=${game.gs.time}", actionFile)

        return points
    }

    private fun selectNewPopulation() {
        candidates.clear()
        candidatesFitnessList.addAll(childrenFitnessList)

        candidatesFitnessList.sortedByDescending { it.fitness }.take(POPULATION).forEach {
            candidates.add(it.decisionMaker)
            it.decisionMaker.setUnused()
        }

        candidatesFitnessList.clear()
        childrenFitnessList.clear()
    }

    /**
     * Tournament selection crossover.
     */
    private fun crossover(): MutableList<DecisionMaker> {
        val children = mutableListOf<DecisionMaker>()
        repeat(POPULATION) {
            val parentCandidates = candidatesFitnessList.shuffled().take(PARENT_COUNT * CANDIDATE_COUNT).chunked(PARENT_COUNT)
            val parents = mutableListOf<EvaluatedCandidate>()
            parentCandidates.forEach {
                parents.add(it.maxBy { it.fitness }!!)
            }
            children.add(parents[0].decisionMaker.crossover(parents[1].decisionMaker))
        }
        return children
    }

    private fun getStats(physicalGameState: PhysicalGameState): List<PlayerStatistics> {
        val players = ArrayList<PlayerStatistics>()
        for (p in physicalGameState.players) {
            val playerStatistics = PlayerStatistics(
                    id = p.id,
                    units = physicalGameState.units.filter { it.player == p.id },
                    hp = physicalGameState.units.filter { it.player == p.id }.sumBy { it.hitPoints },
                    hpBase = physicalGameState.units.filter { it.player == p.id && it.type.name == "Base"}.sumBy { it.hitPoints }
            )
            players.add(playerStatistics)
        }
        return players
    }

    private fun printBestCandidate(epoch: Int) {
        if (epoch % 10 == 0) {
            writeToFile("EPOCH $epoch ------------------------------------------", Utils.conditionsFile)
            writeToFile("fitness=${bestCandidate?.fitness}", Utils.conditionsFile)
            writeToFile(bestCandidate?.decisionMaker.toString(), Utils.conditionsFile)
        }
    }


    private fun printFitnessStats() {
        writeToFile("--- BEST: ${candidatesFitnessList[0].fitness}")
        writeToFile("--- WORST: ${candidatesFitnessList[candidatesFitnessList.size - 1].fitness}")
        writeToFile("--- AVG: ${candidatesFitnessList.sumByDouble { it.fitness } / candidatesFitnessList.size}")
    }
}