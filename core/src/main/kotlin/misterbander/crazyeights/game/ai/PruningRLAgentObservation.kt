package misterbander.crazyeights.game.ai

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.DrawTwoEffectPenalty
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.PassMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit

class PruningRLAgentObservation(
	private val ruleset: Ruleset,
	val playerCardCounts: OrderedMap<Player, Int>,
	drawStackCardCount: Int,
	val topCard: ServerCard,
	private val unseenCards: GdxArray<ServerCard>,
	val observer: Player,
	val observerHand: GdxArray<ServerCard>,
	val currentPlayer: Player,
	private val isPlayReversed: Boolean = false,
	private val drawCount: Int = 0,
	private val declaredSuit: Suit? = null,
	private val drawTwoEffectCardCount: Int = 0
)
{
	val drawStackCardCount = if (drawStackCardCount <= 0) 52 - playerCardCounts.values().sumOf { it } - 1 else drawStackCardCount
	
	private val playerCount: Int
		get() = playerCardCounts.size
	val nextPlayer: Player
		get()
		{
			val keys = playerCardCounts.orderedKeys()
			return keys[(keys.indexOf(currentPlayer, true) + 1)%playerCount]
		}
	val previousPlayer: Player
		get()
		{
			val keys = playerCardCounts.orderedKeys()
			return keys[(keys.indexOf(currentPlayer, true) + playerCount - 1)%playerCount]
		}
	
	val moves = GdxArray<Move>()
	
	init
	{
		val cards = if (currentPlayer == observer) observerHand else unseenCards
		
		if (drawTwoEffectCardCount > 0) // Someone played a 2
		{
			moves += DrawTwoEffectPenalty(drawTwoEffectCardCount)
			for (card: ServerCard in cards)
			{
				if (card.rank == Rank.TWO)
					moves += PlayMove(card)
			}
			
		}
		else
		{
			moves += if (drawCount < 3 && drawStackCardCount > 0) DrawMove else PassMove
			for (card: ServerCard in cards)
			{
				if (card.rank == Rank.EIGHT)
				{
					if (ruleset.declareSuitsOnEights)
					{
						moves += ChangeSuitMove(card, Suit.DIAMONDS)
						moves += ChangeSuitMove(card, Suit.CLUBS)
						moves += ChangeSuitMove(card, Suit.HEARTS)
						moves += ChangeSuitMove(card, Suit.SPADES)
					}
					else
						moves += PlayMove(card)
				}
				else if (card.suit == declaredSuit
					|| declaredSuit == null && card.suit == topCard.suit
					|| card.rank == topCard.rank)
					moves += PlayMove(card)
			}
		}
	}
	
	constructor(
		state: GameState
	) : this(
		state.ruleset,
		OrderedMap(),
		state.drawStack.size,
		state.topCard,
		GdxArray(),
		state.currentPlayer,
		state.currentPlayerHand,
		state.currentPlayer,
		state.isPlayReversed,
		state.drawCount,
		state.declaredSuit,
		state.drawTwoEffectCardCount
	)
	{
		state.playerHands.forEach { (player, hand) -> playerCardCounts[player] = hand!!.size }
		
		unseenCards += state.drawStack
		for ((player, hand) in state.playerHands)
		{
			if (player != state.currentPlayer)
				unseenCards += hand!!
		}
	}
	
	fun afterMove(move: Move): PruningRLAgentObservation
	{
		assert(move in moves) { "Illegal move! Trying $move but legal moves are $moves" }
		val playerCardCounts: OrderedMap<Player, Int> = OrderedMap(this.playerCardCounts)
		return when (move)
		{
			is PlayMove ->
			{
				playerCardCounts[currentPlayer]--
				copy(
					playerCardCounts = playerCardCounts,
					topCard = move.card,
					unseenCards = unseenCards - move.card,
					currentPlayer = nextPlayer,
					drawCount = 0,
					declaredSuit = null,
					drawTwoEffectCardCount = if (ruleset.drawTwos && move.card.rank == Rank.TWO) drawTwoEffectCardCount + 2 else 0
				)
			}
			is DrawMove ->
			{
				playerCardCounts[currentPlayer]++
				copy(
					playerCardCounts = playerCardCounts,
					drawStackCardCount = drawStackCardCount - 1,
					currentPlayer = if (ruleset.passAfterDraw) nextPlayer else currentPlayer,
					drawCount = if (ruleset.passAfterDraw) 0 else drawCount + 1,
				)
			}
			is PassMove -> copy(currentPlayer = nextPlayer)
			is ChangeSuitMove ->
			{
				playerCardCounts[currentPlayer]--
				copy(
					playerCardCounts = playerCardCounts,
					topCard = move.card,
					unseenCards = unseenCards - move.card,
					currentPlayer = nextPlayer,
					drawCount = 0,
					declaredSuit = move.declaredSuit
				)
			}
			is DrawTwoEffectPenalty ->
			{
				playerCardCounts[currentPlayer] += 2
				copy(
					playerCardCounts = playerCardCounts,
					drawStackCardCount = drawStackCardCount - 2,
					currentPlayer = nextPlayer
				)
			}
		}
	}
	
	fun copy(
		playerCardCounts: OrderedMap<Player, Int> = OrderedMap(this.playerCardCounts),
		drawStackCardCount: Int = this.drawStackCardCount,
		topCard: ServerCard = this.topCard,
		unseenCards: GdxArray<ServerCard> = GdxArray(this.unseenCards),
		observer: Player = this.observer,
		observerHand: GdxArray<ServerCard> = GdxArray(this.observerHand),
		currentPlayer: Player = this.currentPlayer,
		isPlayReversed: Boolean = this.isPlayReversed,
		drawCount: Int = this.drawCount,
		declaredSuit: Suit? = this.declaredSuit,
		drawTwoEffectCardCount: Int = this.drawTwoEffectCardCount
	): PruningRLAgentObservation = PruningRLAgentObservation(
		ruleset,
		playerCardCounts,
		drawStackCardCount,
		topCard,
		unseenCards,
		observer,
		observerHand,
		currentPlayer,
		isPlayReversed,
		drawCount,
		declaredSuit,
		drawTwoEffectCardCount
	)
	
	val isTerminal: Boolean
		get()
		{
			for (count in playerCardCounts.values())
			{
				if (count == 0)
					return true
			}
			return false
		}
	
	val result: Double
		get() = if (playerCardCounts[observer] == 0) Double.POSITIVE_INFINITY else Double.NEGATIVE_INFINITY
}
