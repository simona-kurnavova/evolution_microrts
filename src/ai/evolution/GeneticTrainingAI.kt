package ai.evolution

import ai.PassiveAI
import ai.RandomBiasedAI
import ai.core.AI
import ai.evolution.TrainingUtils.ACTIVE_START
import ai.evolution.condition.DecisionMaker
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.PhysicalGameState
import rts.units.UnitTypeTable
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
import ai.evolution.Utils.Companion.writeToFile
import java.lang.Exception

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
        val candidateLists = candidates.chunked(CORES_COUNT)

        candidateLists.forEach { list ->
            var index = Random.nextInt()
            val passiveAIs = mutableListOf<AI>(PassiveAI(UnitTypeTable(gameSettings.uttVersion)),
                    PassiveAI(UnitTypeTable(gameSettings.uttVersion)),
                    RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)))

            val activeAIs = mutableListOf<AI>(RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                    RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)),
                    RandomBiasedAI(UnitTypeTable(gameSettings.uttVersion)))

            val AIs = if (epoch < ACTIVE_START) passiveAIs else activeAIs

            val bestDecisionMaker = bestCandidate?.decisionMaker

            if (bestDecisionMaker != null && epoch >= TOURNAMENT_START) {
                AIs.add(GeneticAI(bestDecisionMaker))
            }

            list.parallelStream().map {
                var fitness = 0.0
                var inconsistentAction = 0
                AIs.forEach {ai ->
                    val player = listOf(0, 1).random()
                    val game = if (player == 0) {
                        Game(gameSettings, GeneticAI(it, null), ai)
                    } else Game(gameSettings, ai, GeneticAI(it, null))

                    try {
                        val actionStatistics = game.start()
                        fitness += calculateFitness(game, actionStatistics[player], it.getCountUsedConditions(), player, epoch, inconsistentAction)
                    } catch (e: Exception) {
                        inconsistentAction ++
                    }
                }
                fitness /= AIs.size // average fitness
                evaluatedCandidates.add(EvaluatedCandidate(it, fitness))
            }.collect(Collectors.toMap({ ++index +  Random.nextInt() }) { p: Any -> p })
        }

        evaluatedCandidates.sortByDescending { it.fitness }
        if (bestCandidate == null || bestCandidate!!.fitness <= evaluatedCandidates[0].fitness) {
            bestCandidate = evaluatedCandidates[0]
        }
        return evaluatedCandidates
    }

    private fun calculateFitness(game: Game, playerStats: ActionStatistics, countUsedConditions: Double, player: Int = 1, epoch: Int, inconsistentAction: Int): Double {
        if (game.gs.winner() == player) {
            return 100000.0
        }
        return when {
            epoch < ACTIVE_START -> {
                playerStats.resHarvested.toDouble() + playerStats.produced + playerStats.resToBase //+ playerStats.moved.toDouble()
            }
            else -> {
                playerStats.resToBase.toDouble() + playerStats.produced + playerStats.resHarvested + playerStats.damageDone
            }
        }
    }

    private fun selectNewPopulation() {
        candidates.clear()
        candidatesFitnessList.take(POPULATION / 2).forEach {
            candidates.add(it.decisionMaker)
            it.decisionMaker.setUnused()
        }
        childrenFitnessList.take(POPULATION / 2).forEach {
            candidates.add(it.decisionMaker)
            it.decisionMaker.setUnused()
        }
    }

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
                    resourceCount = physicalGameState.units.filter { it.player == p.id }.sumBy { it.resources },
                    hp = physicalGameState.units.filter { it.player == p.id }.sumBy { it.hitPoints }
            )
            if (playerStatistics.units.isNullOrEmpty() ||
                    (playerStatistics.units.size == 1 && playerStatistics.units.get(0)?.type?.name == "Base")) {
                playerStatistics.lost = true
            }
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
        writeToFile("--- AVG: ${candidatesFitnessList.sumByDouble { it.fitness } / candidatesFitnessList.size}")
    }
}