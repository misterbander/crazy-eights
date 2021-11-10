package misterbander.crazyeights.game.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectIntMap
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.GameState
import misterbander.crazyeights.game.Move
import misterbander.crazyeights.game.PassMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit

class OracleAgent : Agent
{
	override fun getMove(state: GameState): Move
	{
		val hand = state.currentPlayerHand
		val topCard = state.topCard
		val suitCount = ObjectIntMap<Suit>()
		for (card: ServerCard in hand) // Count suits
		{
			if (card.rank != Rank.EIGHT) // Don't count eights
				suitCount.getAndIncrement(card.suit, 0, 1)
		}
		
		// Someone played a 2
		if (state.ruleset.drawTwos && state.drawTwoEffectCardCount > 0)
		{
			// Counter with another 2 if possible
			for (card in hand)
			{
				if (card.rank == Rank.TWO)
					return PlayMove(card)
			}
			return state.moves[0]
		}
		
		// Check if a smart suit twist can be made, if so then twist the suit
		for (card in hand)
		{
			if (card.rank == topCard.rank && (!state.ruleset.declareSuitsOnEights || card.rank != Rank.EIGHT))
			{
				if (suitCount[card.suit, 0] > suitCount[topCard.suit, 0])
					return PlayMove(card)
			}
		}
		
		// Check for any cards with matching suit
		for (card: ServerCard in hand)
		{
			if ((state.declaredSuit == null && card.suit == topCard.suit || card.suit == state.declaredSuit)
				&& card.rank != Rank.EIGHT)
				return PlayMove(card)
		}
		// Check for any card with matching rank
		for (card: ServerCard in hand)
		{
			if (card.rank == topCard.rank && card.rank != Rank.EIGHT)
				return PlayMove(card)
		}
		// Check for any eights
		for (card in hand)
		{
			if (card.rank != Rank.EIGHT)
				continue
			// Select plentiest suit
			if (state.ruleset.declareSuitsOnEights)
			{
				val declaredSuit = (suitCount.maxOfWithOrNull({ o1, o2 ->
					o1.value - o2.value
				}) { it })?.key ?: Suit.values()[MathUtils.random(1, Suit.values().size - 2)]
				return ChangeSuitMove(card, declaredSuit)
			}
			return PlayMove(card)
		}
		
		if (state.drawCount < 3)
			return DrawMove
		return PassMove
	}
}
