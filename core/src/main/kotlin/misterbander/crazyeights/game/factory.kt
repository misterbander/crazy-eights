package misterbander.crazyeights.game

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.game.ai.Agent
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit

fun newDeck(): GdxArray<ServerCard>
{
	val deck = GdxArray<ServerCard>()
	for (suit in Suit.values())
	{
		if (suit == Suit.NO_SUIT || suit == Suit.JOKER)
			continue
		for (rank in Rank.values())
		{
			if (rank != Rank.NO_RANK)
				deck += ServerCard(rank = rank, suit = suit)
		}
	}
	assert(deck.size == 52) { "Deck is not a proper 52-card deck!" }
	return deck
}

fun shuffledDeck(): GdxArray<ServerCard> = newDeck().apply { shuffle() }

fun newGame(ruleset: Ruleset = Ruleset(), agents: GdxArray<Agent>): GameState
{
	val newDeck = shuffledDeck()
	val playerHands = OrderedMap<Player, GdxArray<ServerCard>>()
	agents.forEach { playerHands[it] = GdxArray() }
	// Deal
	playerHands.values().forEach { hand: GdxArray<ServerCard> -> repeat(7) { hand += newDeck.pop() } }
	// Flip over the top card from the drawing pile to start the discard pile
	val discardPile = gdxArrayOf(newDeck.pop())
	return GameState(ruleset, playerHands, newDeck, discardPile)
}

fun String.toCard(): ServerCard
{
	val rank = when (val rankStr = substring(0..(length - 2)))
	{
		"A" -> Rank.ACE
		"J" -> Rank.JACK
		"Q" -> Rank.QUEEN
		"K" -> Rank.KING
		"2", "3", "4", "5", "6", "7", "8", "9", "10" -> Rank.values()[rankStr.toInt()]
		else -> throw IllegalArgumentException("Incorrect rank format: $this")
	}
	val suit = when (last())
	{
		'♢' -> Suit.DIAMONDS
		'♣' -> Suit.CLUBS
		'♡' -> Suit.HEARTS
		'♠' -> Suit.SPADES
		else -> throw IllegalArgumentException("Incorrect suit format: $this")
	}
	return ServerCard(rank = rank, suit = suit)
}

fun createState(ruleset: Ruleset = Ruleset(), stateStr: String, agent1: Agent, agent2: Agent): GameState
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
	val drawStack = newDeck() - hands[agent1] - hands[agent2] - discardPile
	return GameState(ruleset, playerHands = hands, drawStack = drawStack, discardPile = discardPile)
}
