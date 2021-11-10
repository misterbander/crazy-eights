package misterbander.crazyeights.game

import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Suit

/**
 * A `Move` represents an action that players can make in their turn.
 */
sealed class Move

data class PlayMove(val card: ServerCard) : Move()
{
	override fun toString(): String = "PLAY [${card.name}]"
}

data class ChangeSuitMove(val card: ServerCard, val declaredSuit: Suit) : Move()
{
	override fun toString(): String = "PLAY [${card.name} -> $declaredSuit]"
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
