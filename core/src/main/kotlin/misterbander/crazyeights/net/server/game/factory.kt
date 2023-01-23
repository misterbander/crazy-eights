package misterbander.crazyeights.net.server.game

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.game.ai.Agent
import misterbander.crazyeights.net.server.shuffledDeck

fun newGame(ruleset: Ruleset = Ruleset(), agents: GdxArray<Agent>): ServerGameState
{
	val newDeck = shuffledDeck { -1 }
	val playerHands = OrderedMap<Player, GdxArray<ServerCard>>()
	agents.forEach { playerHands[it] = GdxArray() }
	// Deal
	playerHands.values().forEach { hand: GdxArray<ServerCard> -> repeat(7) { hand += newDeck.pop() } }
	// Flip over the top card from the drawing pile to start the discard pile
	val discardPile = gdxArrayOf(newDeck.pop())
	return ServerGameState(ruleset, playerHands, newDeck, discardPile)
}
