package misterbander.crazyeights.game

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit

/**
 * Mutable state representing a game of Crazy Eights.
 * @property ruleset ruleset used by this game
 * @property playerHands an ordered map mapping player objects to their hands
 * @property drawStack the stack where all players can draw cards from
 * @param discardPile all the cards in the discard pile
 * @param currentPlayer the current player
 * @property isPlayReversed true if the current direction of play is reversed
 * @param drawCount the number of times the current player has drawn from the drawing stack. If the
 * draw count reaches 3, the player can no longer draw.
 * @param declaredSuit if not null, this is the suit the current player must follow instead of the
 * suit of the top card of the discard pile. Used when the previous player plays an eight and declares
 * a suit.
 */
class GameState(
	val ruleset: Ruleset = Ruleset(),
	val playerHands: OrderedMap<Player, GdxArray<ServerCard>>,
	val drawStack: GdxArray<ServerCard>,
	private val discardPile: GdxArray<ServerCard>,
	currentPlayer: Player = playerHands.orderedKeys()[0],
	var isPlayReversed: Boolean = false,
	drawCount: Int = 0,
	declaredSuit: Suit? = null,
	drawTwoEffectCardCount: Int = 0
)
{
	/** Top card of the discard pile. */
	val topCard: ServerCard
		get() = discardPile.peek()
	
	/** Total number of players in the game. */
	val playerCount: Int
		get() = playerHands.size
	var currentPlayer: Player = currentPlayer
		private set
	val currentPlayerHand: GdxArray<ServerCard>
		get() = playerHands[currentPlayer]
	val nextPlayer: Player
		get()
		{
			val keys = playerHands.orderedKeys()
			return if (isPlayReversed)
				keys[(keys.indexOf(currentPlayer, true) + playerCount - 1)%playerCount]
			else
				keys[(keys.indexOf(currentPlayer, true) + 1)%playerCount]
		}
	
	/**
	 * The number of times the current player has drawn from the drawing stack. If the draw count
	 * reaches 3, the player can no longer draw.
	 */
	var drawCount = drawCount
		private set
	/**
	 * If not null, this is the suit the current player must follow instead of the top card of the
	 * discard pile. Used when the previous player plays an eight and declares a suit.
	 */
	var declaredSuit: Suit? = declaredSuit
		private set
	/** Number of cards to be drawn due to the draw two effect. */
	var drawTwoEffectCardCount = drawTwoEffectCardCount
		private set
	
	/** All possible moves from this state. */
	val moves = GdxArray<Move>()
	
	init
	{
		updateMoves()
	}
	
	/**
	 * Executes the specified move and mutates the state.
	 */
	fun doMove(move: Move)
	{
		assert(move in moves) { "Illegal move! $currentPlayer trying to $move but legal moves are $moves" }
		when (move)
		{
			is PlayMove ->
			{
				discard(move.card)
				declaredSuit = null
				if (ruleset.drawTwos && move.card.rank == Rank.TWO)
					drawTwoEffectCardCount += 2
				else
					drawTwoEffectCardCount = 0
				if (ruleset.skips && move.card.rank == Rank.QUEEN)
					advanceToNextPlayer()
				advanceToNextPlayer()
			}
			is DrawMove ->
			{
				drawOne()
				if (ruleset.passAfterDraw)
					advanceToNextPlayer()
			}
			is PassMove -> advanceToNextPlayer()
			is ChangeSuitMove ->
			{
				val (card, declaredSuit) = move
				discard(card)
				this.declaredSuit = declaredSuit
				advanceToNextPlayer()
			}
			is DrawTwoEffectPenalty ->
			{
				repeat(move.cardCount) {
					if (drawStack.isNotEmpty())
						drawOne()
					if (drawStack.isEmpty)
						refillDrawingStack()
				}
				drawTwoEffectCardCount = 0
				advanceToNextPlayer()
			}
		}
		if (drawStack.isEmpty)
			refillDrawingStack()
		
		updateMoves()
		
		val allPlayerCardCount = playerHands.values().sumOf { it.size }
		val discardedCardCount = discardPile.size
		val drawingStackCardCount = drawStack.size
		val totalCardCount = allPlayerCardCount + discardedCardCount + drawingStackCardCount
		assert(totalCardCount == 52) { "Cards magically appeared/disappeared! $totalCardCount" }
	}
	
	private fun updateMoves()
	{
		moves.clear()
		
		// Someone played a 2
		if (drawTwoEffectCardCount > 0)
		{
			moves += DrawTwoEffectPenalty(drawTwoEffectCardCount)
			for (card: ServerCard in currentPlayerHand)
			{
				if (card.rank == Rank.TWO)
					moves += PlayMove(card)
			}
			return
		}
		
		moves += if (drawCount < 3 && drawStack.isNotEmpty()) DrawMove else PassMove
		for (card: ServerCard in currentPlayerHand)
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
	
	private fun discard(card: ServerCard)
	{
		currentPlayerHand -= card
		discardPile += card
	}
	
	private fun drawOne()
	{
		currentPlayerHand += drawStack.pop()
		drawCount++
	}
	
	private fun refillDrawingStack()
	{
		val topCard = this.topCard
		discardPile -= topCard
		discardPile.shuffle()
		drawStack += discardPile
		discardPile.clear()
		discardPile += topCard
	}
	
	/**
	 * Advances to the next player.
	 */
	private fun advanceToNextPlayer()
	{
		currentPlayer = nextPlayer
		drawCount = 0
	}
	
	val observedState: GameState
		get()
		{
			// From the point of view of the current player, they can only see their own hand
			// and all discarded cards
			val unseenCards = GdxArray<ServerCard>()
			unseenCards += drawStack
			for ((player, cards) in playerHands)
			{
				if (player != currentPlayer)
					unseenCards += cards!!
			}
			unseenCards.shuffle()
			val observedHands = OrderedMap<Player, GdxArray<ServerCard>>()
			val observedDrawingStack = GdxArray<ServerCard>()
			
			// Deal the unseen cards to the other players
			repeat(drawStack.size) { observedDrawingStack += unseenCards.pop() }
			for ((player, cards) in playerHands)
			{
				if (player == currentPlayer)
					observedHands[player] = GdxArray(cards)
				else
				{
					val hand = GdxArray<ServerCard>()
					repeat(cards!!.size) { hand += unseenCards.pop() }
					observedHands[player] = hand
				}
			}
			assert(unseenCards.isEmpty)
			
			return GameState(
				ruleset,
				observedHands,
				observedDrawingStack,
				GdxArray(discardPile),
				currentPlayer,
				isPlayReversed,
				drawCount,
				declaredSuit,
				drawTwoEffectCardCount
			)
		}
	
	val isTerminal: Boolean
		get()
		{
			for (player: Player in playerHands.keys())
			{
				if (getResult(player) == 1)
					return true
			}
			return false
		}
	
	fun getResult(player: Player): Int = if (playerHands[player].isEmpty) 1 else 0
}
