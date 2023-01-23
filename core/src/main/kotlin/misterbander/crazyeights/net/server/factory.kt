package misterbander.crazyeights.net.server

import com.badlogic.gdx.math.MathUtils
import ktx.collections.*
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.gframework.util.shuffle

inline fun newDeck(idProvider: () -> Int): GdxArray<ServerCard>
{
	val deck = GdxArray<ServerCard>()
	for (suit in Suit.values())
	{
		if (suit == Suit.NO_SUIT || suit == Suit.JOKER)
			continue
		for (rank in Rank.values())
		{
			if (rank != Rank.NO_RANK)
				deck += ServerCard(idProvider(), rank = rank, suit = suit)
		}
	}
	assert(deck.size == 52) { "Deck is not a proper 52-card deck!" }
	return deck
}

inline fun shuffledDeck(seed: Long = MathUtils.random.nextLong(), idProvider: () -> Int): GdxArray<ServerCard> =
	newDeck(idProvider).apply { shuffle(seed) }

fun String.toCard(id: Int = -1): ServerCard
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
	return ServerCard(id, rank = rank, suit = suit)
}

inline fun newServerTabletop(
	server: CrazyEightsServer,
	playerHands: Map<User, Array<String>> = emptyMap(),
	drawStack: Array<String> = emptyArray(),
	discardPile: Array<String> = emptyArray(),
	idProvider: () -> Int
): ServerTabletop
{
	val handsConverted = playerHands.mapValues { (_, value) ->
		value.map { it.toCard(idProvider()) }.toGdxArray<ServerObject>()
	}
	val drawStackHolder = ServerCardHolder(
		idProvider(),
		cardGroup = ServerCardGroup(idProvider(), cards = drawStack.map { it.toCard(idProvider()) }.toGdxArray())
	)
	val discardPileHolder = ServerCardHolder(
		idProvider(),
		cardGroup = ServerCardGroup(idProvider(), cards = discardPile.map { it.toCard(idProvider()) }.toGdxArray())
	)
	return ServerTabletop(server, handsConverted, drawStackHolder, discardPileHolder)
}
