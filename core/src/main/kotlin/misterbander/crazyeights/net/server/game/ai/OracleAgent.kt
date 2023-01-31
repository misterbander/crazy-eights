package misterbander.crazyeights.net.server.game.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.ObjectIntMap
import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.crazyeights.net.server.game.ChangeSuitMove
import misterbander.crazyeights.net.server.game.DrawMove
import misterbander.crazyeights.net.server.game.Move
import misterbander.crazyeights.net.server.game.PassMove
import misterbander.crazyeights.net.server.game.PlayMove
import misterbander.crazyeights.net.server.game.ServerGameState

class OracleAgent(override val name: String = "OracleAgent") : Agent
{
	override fun getMove(state: ServerGameState): Move
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
		if (state.ruleset.drawTwos != null && state.drawTwoEffectCardCount > 0)
		{
			// Counter with another 2 if possible
			for (card in hand)
			{
				if (card.rank == state.ruleset.drawTwos)
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
		
		if (state.drawCount < state.ruleset.maxDrawCount)
			return DrawMove()
		return PassMove
	}
}
