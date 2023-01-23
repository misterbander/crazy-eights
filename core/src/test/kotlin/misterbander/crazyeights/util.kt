package misterbander.crazyeights

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.game.Player
import misterbander.crazyeights.net.server.game.Ruleset
import misterbander.crazyeights.net.server.game.ServerGameState
import misterbander.crazyeights.net.server.game.ai.Agent
import misterbander.crazyeights.net.server.game.ai.BetterRandomAgent
import misterbander.crazyeights.net.server.game.ai.OracleAgent
import misterbander.crazyeights.net.server.newDeck
import misterbander.crazyeights.net.server.toCard
import java.io.File

val priorityList = arrayOf("3♡", "7♡", "4♠", "7♢", "7♠", "3♢", "6♢", "10♠", "5♢", "8♣", "4♡", "Q♡", "A♢", "6♡", "10♢", "10♣", "2♠", "J♢", "Q♣", "8♡", "5♠", "9♢", "8♢", "Q♠", "2♢", "6♠", "A♣", "2♡", "A♠", "7♣", "4♢", "10♡", "2♣", "K♣", "J♡", "Q♢", "9♠", "6♣", "4♣", "3♣", "J♠", "J♣", "K♢", "9♡", "8♠", "5♡", "9♣", "A♡", "K♠", "K♡", "5♣", "3♠")
val agent1 = OracleAgent()
val agent2 = BetterRandomAgent()

fun main()
{
	val states = readTestStates()
	for (state in states)
		println(agent1.getMove(state))
}

fun readTestStates(ruleset: Ruleset = Ruleset()): Array<ServerGameState>
{
	val states = GdxArray<ServerGameState>()
	File("src/test/resources/random_states.txt").forEachLine { line ->
		states += createGameState(ruleset, stateStr = line, agent1 = agent1, agent2 = agent2)
	}
	return states.toArray(ServerGameState::class.java)
}

fun createGameState(ruleset: Ruleset = Ruleset(), stateStr: String, agent1: Agent, agent2: Agent): ServerGameState
{
	val cards = stateStr.split("/").map { listStr ->
		listStr
			.substring(1..(listStr.length - 2))
			.replace(" ", "")
			.split(",")
			.map { it.toCard() }
	}
	val hands = OrderedMap<Player, GdxArray<ServerCard>>()
	hands[agent1] = cards[0].toGdxArray()
	hands[agent2] = cards[1].toGdxArray()
	val discardPile = cards[2].toGdxArray()
	val drawStack = newDeck { -1 } - hands[agent1]!! - hands[agent2]!! - discardPile
	return ServerGameState(ruleset, playerHands = hands, drawStack = drawStack, discardPile = discardPile)
}
