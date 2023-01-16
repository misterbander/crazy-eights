package misterbander.crazyeights.net.server.game

import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.ServerCard.Suit

/**
 * A `Move` represents an action that players can make in their turn.
 */
sealed class Move

data class PlayMove(val card: ServerCard) : Move()
{
	override fun toString(): String = "PLAY [${card.name}]"
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as PlayMove
		
		if (card.rank != other.card.rank)
			return false
		if (card.suit != other.card.suit)
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = card.rank.hashCode()
		result = 31*result + card.suit.hashCode()
		return result
	}
}

data class ChangeSuitMove(val card: ServerCard, val declaredSuit: Suit) : Move()
{
	override fun toString(): String = "PLAY [${card.name} -> $declaredSuit]"
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as ChangeSuitMove
		
		if (card.rank != other.card.rank)
			return false
		if (card.suit != other.card.suit)
			return false
		if (declaredSuit != other.declaredSuit)
			return false
		
		return true
	}
	
	override fun hashCode(): Int
	{
		var result = card.rank.hashCode()
		result = 31*result + card.suit.hashCode()
		result = 31*result + declaredSuit.hashCode()
		return result
	}
}

object DrawMove : Move()
{
	override fun toString(): String = "DRAW"
}

object PassMove : Move()
{
	override fun toString(): String = "PASS"
}

data class DrawTwoEffectPenalty(val cardCount: Int) : Move()
{
	override fun toString(): String = "DRAW $cardCount"
}
