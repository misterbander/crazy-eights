package misterbander.crazyeights.game.ai

import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.OrderedMap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import ktx.collections.*
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.newGame
import misterbander.crazyeights.model.ServerCard.Rank
import java.util.concurrent.Executors

val reducedRuleset = Ruleset(passAfterDraw = true, declareSuitsOnEights = false)
val basicRuleset = Ruleset()
val variantRuleset = Ruleset(drawTwos = Rank.TWO, skips = Rank.QUEEN, reverses = Rank.ACE)

fun main()
{
	simulateGames(basicRuleset, { arrayOf(IsmctsAgent(), OracleAgent()) }, randomizeAgents = true)
}

fun simulateGames(
	ruleset: Ruleset = basicRuleset,
	agentsProvider: () -> Array<Agent>,
	gamesCount: Int = 10000,
	threadCount: Int = 12,
	randomizeAgents: Boolean = false,
	verbose: Boolean = false
) = runBlocking {
	val executorService = Executors.newFixedThreadPool(threadCount)
	val context = executorService.asCoroutineDispatcher()
	val agents = agentsProvider()
	val agentNames = Array(agents.size) { i -> "${agents[i].name} $i" }
	
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
	runGameJobs.forEachIndexed { index, result ->
		val winner = result.await()
		winCount.getAndIncrement(winner, 0, 1)
		println("Game ${index}: $winCount")
	}
	println(winCount)
	executorService.shutdown()
}

private fun simulateGame(
	ruleset: Ruleset = basicRuleset,
	agents: OrderedMap<Agent, String>,
	verbose: Boolean
): Agent
{
	val game: ServerGameState = newGame(ruleset = ruleset, agents.orderedKeys())
	while (!game.isTerminal)
	{
		val currentPlayer = game.currentPlayer as Agent
		val currentPlayerName = agents[currentPlayer]!!
		if (game.drawCount == 0 && verbose)
			println(
				"$currentPlayerName's (${game.currentPlayerHand.size}) turn! ${game.currentPlayerHand.map { it.name }}\n"
					+ "Top card = ${game.topCard.name}\n"
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
