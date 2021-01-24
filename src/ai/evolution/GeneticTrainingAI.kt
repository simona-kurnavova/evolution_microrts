package ai.evolution

import ai.evolution.TrainingUtils.ACTIVE_START
import ai.evolution.TrainingUtils.BEST_AI_EPOCH
import ai.evolution.TrainingUtils.CANDIDATE_COUNT
import ai.evolution.TrainingUtils.CONDITION_COUNT
import ai.evolution.TrainingUtils.CORES_COUNT
import ai.evolution.TrainingUtils.EPOCH_COUNT
import ai.evolution.TrainingUtils.HEADLESS
import ai.evolution.TrainingUtils.MAP_LOCATION
import ai.evolution.TrainingUtils.MAX_CYCLES
import ai.evolution.Utils.Companion.EvaluatedCandidate
import ai.evolution.TrainingUtils.PARENT_COUNT
import ai.evolution.TrainingUtils.PARTIALLY_OBSERVABLE
import ai.evolution.TrainingUtils.POPULATION
import ai.evolution.TrainingUtils.SLOW_TESTING
import ai.evolution.Utils.Companion.PlayerStatistics
import ai.evolution.TrainingUtils.TESTING_RUNS
import ai.evolution.TrainingUtils.UPDATE_INTERVAL
import ai.evolution.TrainingUtils.UTT_VERSION
import ai.evolution.TrainingUtils.getActiveAIS
import ai.evolution.TrainingUtils.getFastTestingAIs
import ai.evolution.TrainingUtils.getPassiveAIS
import ai.evolution.TrainingUtils.getSlowTestingAIs
import ai.evolution.TrainingUtils.printInfo
import ai.evolution.Utils.Companion.actionFile
import ai.evolution.Utils.Companion.conditionsFile
import ai.evolution.Utils.Companion.evalFile
import ai.evolution.Utils.Companion.writeToFile
import ai.evolution.condition.DecisionMaker
import com.google.gson.Gson
import rts.ActionStatistics
import rts.Game
import rts.GameSettings
import rts.PhysicalGameState
import rts.units.UnitTypeTable
import java.io.File
import java.util.stream.Collectors
import kotlin.math.abs
import kotlin.random.Random


class GeneticTrainingAI(val gameSettings: GameSettings) {
    
    private var candidates = mutableListOf<DecisionMaker>()
    private var candidatesFitnessList = mutableListOf<EvaluatedCandidate>()
    private var childrenFitnessList = mutableListOf<EvaluatedCandidate>()
    private var bestCandidate: EvaluatedCandidate? = null

    fun train() {
        writeToFile(printInfo())
        initialisePopulation()

        for (epoch in 0 until EPOCH_COUNT) {
            writeToFile("Epoch $epoch")
            candidatesFitnessList = evaluateFitness(candidates, epoch) // Evaluate fitness of candidates
            printFitnessStats()

            val children = crossover() // Crossover between candidates
            children.forEach { it.mutate() } // Mutate children
            childrenFitnessList = evaluateFitness(children, epoch) // Evaluate children
            writeToFile("--- CHILDREN: ${childrenFitnessList.sumByDouble { it.fitness } / childrenFitnessList.size} " +
                    "wins: ${childrenFitnessList.sumByDouble { it.wins.toDouble() } / childrenFitnessList.size}")

            if (epoch >= BEST_AI_EPOCH && saveBestIfFound()) return
            selectNewPopulation() // Selection
        }
    }

    fun loadAIFromFile(file: File = conditionsFile): DecisionMaker {
        // read whole file and remove whitespaces
        val text = file.readText(Charsets.UTF_8).replace("\\s+".toRegex(), " ")
        // parse to DecisionMaker class
        return Gson().fromJson(text, DecisionMaker::class.java)
    }

    /**
     * Parses unit from specified file and evaluates it.
     */
    fun evaluateUnitFromFile(file: File = conditionsFile) {
        // Run tests
        testBestAI(loadAIFromFile(file))
    }

    private fun saveBestIfFound(): Boolean {
        val best = bestCandidate ?: candidatesFitnessList[0]
        if (best.wins >= getActiveAIS(gameSettings).size) {
            writeToFile("Found best unit of Fitness: " + "${best.fitness}")

            // Save best unit to file
            conditionsFile.delete() // delete existig file first
            writeToFile(Gson().toJson(best.decisionMaker).toString(), conditionsFile)
            return true
        }
        return false
    }

    /**
     * Initialise random population of [POPULATION] size with [CONDITION_COUNT] number of conditions for each unit.
     */
    private fun initialisePopulation() {
        repeat(POPULATION) { candidates.add(DecisionMaker(CONDITION_COUNT)) }
    }

    private fun evaluateFitness(candidates: MutableList<DecisionMaker>, epoch: Int): MutableList<EvaluatedCandidate> {
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
                var wins = 0
                AIs.forEach { ai ->
                    val player = listOf(0, 1).random()
                    val game = if (player == 0) {
                        Game(gameSettings, GeneticAI(it, UnitTypeTable(gameSettings.uttVersion)), ai.first)
                    } else Game(gameSettings, ai.first, GeneticAI(it, UnitTypeTable(gameSettings.uttVersion)))

                    try {
                        val actionStatistics = game.start()
                        val fitnessEval = calculateFitness(game, actionStatistics[player], player, epoch)
                        fitness += fitnessEval.first
                        if (fitnessEval.second)
                            wins += 1
                    } catch (e: Exception) {
                        writeToFile("error: ${e.message}")
                    }
                }
                fitness /= AIs.size.toDouble() // average fitness
                evaluatedCandidates.add(EvaluatedCandidate(it, fitness, wins))
            }.collect(Collectors.toMap({ ++index + Random.nextInt() }) { p: Any -> p })
        }

        evaluatedCandidates.sortByDescending{ it.wins; it.fitness }
        evaluatedCandidates.forEach {
            writeToFile("${it.wins}, ${it.fitness}", actionFile)
        }
        if (bestCandidate == null || bestCandidate!!.fitness <= evaluatedCandidates[0].fitness && epoch >= ACTIVE_START) {
            bestCandidate = evaluatedCandidates[0]
        }
        return evaluatedCandidates
    }

    private fun calculateFitness(game: Game, playerStats: ActionStatistics, player: Int = 1, epoch: Int? = null): Pair<Double, Boolean> {
        val stats = getStats(game.gs.physicalGameState)
        val hp = stats[player].hp.toDouble() - stats[abs(player - 1)].hp

        val hpBase = stats[player].hpBase.toDouble() - stats[abs(player - 1)].hpBase

        var points = if (epoch != null && epoch < ACTIVE_START) playerStats.produced.toDouble() + (if (playerStats.barracks) 5 else 0)
            else ((playerStats.damageDone.toDouble() + playerStats.produced + 1) - (playerStats.enemyDamage + 1)) + hp + (hpBase * 10)

        if (epoch == null || epoch >= ACTIVE_START)
            if (game.gs.winner() == player) {
                points += 300000 / game.gs.time
                //writeToFile("WIN = $points, time=${game.gs.time}")
            }

        return Pair(points, game.gs.winner() == player)
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
                parents.add(it.maxByOrNull { it.fitness }!!)
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
                    hpBase = physicalGameState.units.filter { it.player == p.id && it.type.name == "Base" }.sumBy { it.hitPoints }
            )
            players.add(playerStatistics)
        }
        return players
    }

    private fun testBestAI(decisionMaker: DecisionMaker) {
        val AIs = getFastTestingAIs()
        if (SLOW_TESTING) AIs.addAll(getSlowTestingAIs())

        writeToFile(Gson().toJson(decisionMaker).toString(), evalFile)

        AIs.forEach { ai ->
            var wins = 0
            var fitness = 0.0
            var bestFitness = -90000.0

            repeat(TESTING_RUNS) {
                val geneticAI = GeneticAI(decisionMaker, UnitTypeTable(gameSettings.uttVersion))
                val game = Game(UnitTypeTable(UTT_VERSION), MAP_LOCATION, HEADLESS, PARTIALLY_OBSERVABLE, MAX_CYCLES,
                        UPDATE_INTERVAL, geneticAI, ai, it % 2 == 1)

                val fitnessEval = runGame(game, it % 2)
                if (fitnessEval != null) {
                    fitness += fitnessEval.first
                    wins += if (fitnessEval.second) 1 else 0
                    if (fitnessEval.first > bestFitness)
                        bestFitness = fitnessEval.first
                }
            }
            writeToFile("Against AI: ${ai}", evalFile)
            writeToFile(" - AVG fitness: ${fitness / TESTING_RUNS}", evalFile)
            writeToFile(" - Best fitness: $bestFitness", evalFile)
            writeToFile(" - Wins: $wins / $TESTING_RUNS", evalFile)
        }
    }

    fun runAIFromFile(file: File = conditionsFile, headless: Boolean = false) {
        val decisionMaker = loadAIFromFile(file)
        val geneticAI = GeneticAI(decisionMaker, UnitTypeTable(gameSettings.uttVersion))
        val game = Game(UnitTypeTable(UTT_VERSION), MAP_LOCATION, headless, PARTIALLY_OBSERVABLE, MAX_CYCLES,
                UPDATE_INTERVAL, geneticAI, getFastTestingAIs()[0], false)
        val fitness = runGame(game, 0)
        if(fitness != null) {
            println("Result: ${fitness.first}, won=${fitness.second}")
        }
    }

    private fun runGame(game: Game, player: Int): Pair<Double, Boolean>? {
        try {
            return calculateFitness(game, game.start()[player], player)
        } catch (e: Exception) {
            writeToFile("error: ${e.message}")
        }
        return null
    }

    private fun printFitnessStats() {
        writeToFile("--- BEST: ${candidatesFitnessList[0].fitness} wins: ${candidatesFitnessList[0].wins}")
        writeToFile("--- WORST: ${candidatesFitnessList[candidatesFitnessList.size - 1].fitness}")
        writeToFile("--- AVG: ${
            candidatesFitnessList.sumByDouble { it.fitness } /
                    candidatesFitnessList.size
        } wins: ${candidatesFitnessList.sumByDouble { it.wins.toDouble() } / candidatesFitnessList.size}")
    }
}