package misterbander.crazyeights.net.server.game

import com.badlogic.gdx.math.MathUtils
import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.ServerCard.Suit

/**
 * A `Move` represents an action that players can make in their turn.
 */
sealed class Move

data class PlayMove(val card: ServerCard, val seed: Long = MathUtils.random.nextLong()) : Move()
{
	override fun toString(): String = "PLAY [${card.name}]"
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as PlayMove
		
		return card.rank == other.card.rank && card.suit == other.card.suit
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
		
		return card.rank == other.card.rank && card.suit == other.card.suit && declaredSuit == other.declaredSuit
	}
	
	override fun hashCode(): Int
	{
		var result = card.rank.hashCode()
		result = 31*result + card.suit.hashCode()
		result = 31*result + declaredSuit.hashCode()
		return result
	}
}

data class DrawMove(val seed: Long = MathUtils.random.nextLong()) : Move()
{
	override fun toString(): String = "DRAW"
	
	override fun equals(other: Any?): Boolean = this === other || javaClass == other?.javaClass
	
	override fun hashCode(): Int = 0
}

object PassMove : Move()
{
	override fun toString(): String = "PASS"
}

data class DrawTwoEffectPenalty(val cardCount: Int, val seed: Long = MathUtils.random.nextLong()) : Move()
{
	override fun toString(): String = "DRAW $cardCount"
	
	override fun equals(other: Any?): Boolean
	{
		if (this === other)
			return true
		if (javaClass != other?.javaClass)
			return false
		
		other as DrawTwoEffectPenalty
		
		return cardCount == other.cardCount
	}
	
	override fun hashCode(): Int = cardCount
}
