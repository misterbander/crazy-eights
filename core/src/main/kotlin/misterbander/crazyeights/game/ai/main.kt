package misterbander.crazyeights.game.ai

import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.OrderedMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import ktx.collections.*
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.game.newGame
import java.io.BufferedWriter
import java.io.FileWriter
import java.util.concurrent.Executors

val reducedRuleset = Ruleset(passAfterDraw = true, declareSuitsOnEights = false)
val basicRuleset = Ruleset()
val variantRuleset = Ruleset(drawTwos = true, skips = true, reverses = true)

fun main()
{
//	val agents = OrderedMap<Agent, String>()
//	agents[ManualAgent()] = "Me"
//	agents[OracleAgent()] = "Oracle"
//	simulateGame(variantRuleset, agents, verbose = true)

//	val dir = "evaluation/reduced/2p/1v1/"
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       RandomAgent())       }, resultsFilename = "${dir}random_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir}random_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       OracleAgent())       }, resultsFilename = "${dir}random_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       PruningRLAgent())    }, resultsFilename = "${dir}random_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       IsmctsAgent())       }, resultsFilename = "${dir}random_vs_ismcts.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), RandomAgent())       }, resultsFilename = "${dir}better_random_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, resultsFilename = "${dir}better_random_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), OracleAgent())       }, resultsFilename = "${dir}better_random_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), PruningRLAgent())    }, resultsFilename = "${dir}better_random_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), IsmctsAgent())       }, resultsFilename = "${dir}better_random_vs_ismcts.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       RandomAgent())       }, resultsFilename = "${dir}oracle_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir}oracle_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       OracleAgent())       }, resultsFilename = "${dir}oracle_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       PruningRLAgent())    }, resultsFilename = "${dir}oracle_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       IsmctsAgent())       }, resultsFilename = "${dir}oracle_vs_ismcts.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    RandomAgent())       }, resultsFilename = "${dir}rl_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, resultsFilename = "${dir}rl_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    OracleAgent())       }, resultsFilename = "${dir}rl_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, resultsFilename = "${dir}rl_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    IsmctsAgent())       }, resultsFilename = "${dir}rl_vs_ismcts.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       RandomAgent())       }, resultsFilename = "${dir}ismcts_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir}ismcts_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       OracleAgent())       }, resultsFilename = "${dir}ismcts_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, resultsFilename = "${dir}ismcts_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, resultsFilename = "${dir}ismcts_vs_ismcts.csv")

//	val dir2 = "evaluation/reduced/2p/1v1_randomized/"
//	simulateGames(reducedRuleset, { arrayOf(RandomAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}random_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}better_random_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir2}better_random_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}oracle_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir2}oracle_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(OracleAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}oracle_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}rl_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir2}rl_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}rl_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir2}rl_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}ismcts_vs_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir2}ismcts_vs_better_random.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}ismcts_vs_oracle.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir2}ismcts_vs_rl.csv")
//	simulateGames(reducedRuleset, { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, randomizeAgents = true, resultsFilename = "${dir2}ismcts_vs_ismcts.csv")

//	val dir3 = "evaluation/basic/2p/1v1/"
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       RandomAgent())       }, resultsFilename = "${dir3}random_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir3}random_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       OracleAgent())       }, resultsFilename = "${dir3}random_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       PruningRLAgent())    }, resultsFilename = "${dir3}random_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       IsmctsAgent())       }, resultsFilename = "${dir3}random_vs_ismcts.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), RandomAgent())       }, resultsFilename = "${dir3}better_random_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, resultsFilename = "${dir3}better_random_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), OracleAgent())       }, resultsFilename = "${dir3}better_random_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), PruningRLAgent())    }, resultsFilename = "${dir3}better_random_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), IsmctsAgent())       }, resultsFilename = "${dir3}better_random_vs_ismcts.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       RandomAgent())       }, resultsFilename = "${dir3}oracle_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir3}oracle_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       OracleAgent())       }, resultsFilename = "${dir3}oracle_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       PruningRLAgent())    }, resultsFilename = "${dir3}oracle_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       IsmctsAgent())       }, resultsFilename = "${dir3}oracle_vs_ismcts.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    RandomAgent())       }, resultsFilename = "${dir3}rl_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, resultsFilename = "${dir3}rl_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    OracleAgent())       }, resultsFilename = "${dir3}rl_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, resultsFilename = "${dir3}rl_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    IsmctsAgent())       }, resultsFilename = "${dir3}rl_vs_ismcts.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       RandomAgent())       }, resultsFilename = "${dir3}ismcts_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir3}ismcts_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       OracleAgent())       }, resultsFilename = "${dir3}ismcts_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, resultsFilename = "${dir3}ismcts_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, resultsFilename = "${dir3}ismcts_vs_ismcts.csv")

//	val dir4 = "evaluation/basic/2p/1v1_randomized/"
//	simulateGames(agentsProvider = { arrayOf(RandomAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}random_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}better_random_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir4}better_random_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}oracle_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir4}oracle_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(OracleAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}oracle_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}rl_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir4}rl_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}rl_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir4}rl_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}ismcts_vs_random.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir4}ismcts_vs_better_random.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}ismcts_vs_oracle.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir4}ismcts_vs_rl.csv")
//	simulateGames(agentsProvider = { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, randomizeAgents = true, resultsFilename = "${dir4}ismcts_vs_ismcts.csv")
	
	val dir5 = "evaluation/variant/2p/1v1/"
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       RandomAgent())       }, resultsFilename = "${dir5}random_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir5}random_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       OracleAgent())       }, resultsFilename = "${dir5}random_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       PruningRLAgent())    }, resultsFilename = "${dir5}random_vs_rl.csv")
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       IsmctsAgent())       }, resultsFilename = "${dir5}random_vs_ismcts.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), RandomAgent())       }, resultsFilename = "${dir5}better_random_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, resultsFilename = "${dir5}better_random_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), OracleAgent())       }, resultsFilename = "${dir5}better_random_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), PruningRLAgent())    }, resultsFilename = "${dir5}better_random_vs_rl.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), IsmctsAgent())       }, resultsFilename = "${dir5}better_random_vs_ismcts.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       RandomAgent())       }, resultsFilename = "${dir5}oracle_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir5}oracle_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       OracleAgent())       }, resultsFilename = "${dir5}oracle_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       PruningRLAgent())    }, resultsFilename = "${dir5}oracle_vs_rl.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       IsmctsAgent())       }, resultsFilename = "${dir5}oracle_vs_ismcts.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    RandomAgent())       }, resultsFilename = "${dir5}rl_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, resultsFilename = "${dir5}rl_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    OracleAgent())       }, resultsFilename = "${dir5}rl_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, resultsFilename = "${dir5}rl_vs_rl.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    IsmctsAgent())       }, resultsFilename = "${dir5}rl_vs_ismcts.csv")
//	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       RandomAgent())       }, resultsFilename = "${dir5}ismcts_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, resultsFilename = "${dir5}ismcts_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       OracleAgent())       }, resultsFilename = "${dir5}ismcts_vs_oracle.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, resultsFilename = "${dir5}ismcts_vs_rl.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, resultsFilename = "${dir5}ismcts_vs_ismcts.csv")
	
	val dir6 = "evaluation/variant/2p/1v1_randomized/"
//	simulateGames(variantRuleset, { arrayOf(RandomAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}random_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}better_random_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(BetterRandomAgent(), BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir6}better_random_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}oracle_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir6}oracle_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(OracleAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}oracle_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}rl_vs_random.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir6}rl_vs_better_random.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}rl_vs_oracle.csv")
//	simulateGames(variantRuleset, { arrayOf(PruningRLAgent(),    PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir6}rl_vs_rl.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       RandomAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}ismcts_vs_random.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       BetterRandomAgent()) }, randomizeAgents = true, resultsFilename = "${dir6}ismcts_vs_better_random.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       OracleAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}ismcts_vs_oracle.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       PruningRLAgent())    }, randomizeAgents = true, resultsFilename = "${dir6}ismcts_vs_rl.csv")
	simulateGames(variantRuleset, { arrayOf(IsmctsAgent(),       IsmctsAgent())       }, randomizeAgents = true, resultsFilename = "${dir6}ismcts_vs_ismcts.csv")
}

fun simulateGames(
	ruleset: Ruleset = basicRuleset,
	agentsProvider: () -> Array<Agent>,
	gamesCount: Int = 10000,
	threadCount: Int = 12,
	randomizeAgents: Boolean = false,
	verbose: Boolean = false,
	resultsFilename: String? = null
) = runBlocking {
	val executorService = Executors.newFixedThreadPool(threadCount)
	val context = executorService.asCoroutineDispatcher()
	val agents = agentsProvider()
	val agentNames = Array(agents.size) { i -> "${agents[i]::class.java.simpleName} $i" }
	
	val runGameJobs = Array(gamesCount) {
		async(context) {
			val agentsCopy = agentsProvider()
			val agentNameMap = OrderedMap<Agent, String>()
			agentsCopy.forEachIndexed { index, agent -> agentNameMap[agent] = agentNames[index] }
			if (randomizeAgents)
				agentNameMap.orderedKeys().shuffle()
			agentNameMap[simulateGame(ruleset, agentNameMap, verbose = verbose)]!!
		}
	}
	
	val winCount = ObjectIntMap<String>()
	val writer = if (resultsFilename != null) BufferedWriter(FileWriter(resultsFilename)) else null
	writer?.write("${agentNames.joinToString(separator = ",")}\n")
	runGameJobs.forEachIndexed { index, result ->
		val winner = result.await()
		winCount.getAndIncrement(winner, 0, 1)
		println("Game ${index}: $winCount")
		
		writer?.apply {
			write("${agentNames.map { winCount[it, 0] }.joinToString(separator = ",")}\n")
			flush()
		}
	}
	println(winCount)
	executorService.shutdown()
	writer?.close()
}

private fun simulateGame(
	ruleset: Ruleset = basicRuleset,
	agents: OrderedMap<Agent, String>,
	verbose: Boolean
): Agent
{
	val game: GameState = newGame(ruleset = ruleset, agents.orderedKeys())
	while (!game.isTerminal)
	{
		val currentPlayer = game.currentPlayer as Agent
		val currentPlayerName = agents[currentPlayer]
		if (game.drawCount == 0 && verbose)
			println(
				"$currentPlayerName's (${game.currentPlayerHand.size}) turn! ${game.currentPlayerHand.map { card -> card.toFriendlyString() }}\n"
					+ "Top card = ${game.topCard.toFriendlyString()}\n"
					+ "Draw stack = ${game.drawStack.size}"
			)
		val move = currentPlayer.getMove(game)
		
		if (verbose)
			println("$currentPlayerName makes move: $move")
		
		game.doMove(move)
	}
	for (agent in game.playerHands.orderedKeys())
	{
		if (game.getResult(agent) == 1)
			return agent as Agent
	}
	throw IllegalStateException("No winner!")
}
