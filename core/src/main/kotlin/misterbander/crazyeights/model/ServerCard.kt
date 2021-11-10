package misterbander.crazyeights.model

data class ServerCard(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	val rank: Rank = Rank.NO_RANK,
	val suit: Suit = Suit.NO_SUIT,
	var isFaceUp: Boolean = false,
	override var lockHolder: User? = null
) : ServerObject, ServerLockable
{
	var cardGroupId: Int = -1
	
	val name: String = if (suit == Suit.JOKER) "JOKER" else "$rank$suit"
	
	override fun toString(): String = "ServerCard($name, id=$id, x=$x, y=$y, rotation=$rotation, isFaceUp=$isFaceUp, lockHolder=$lockHolder)"
	
	enum class Rank
	{
		NO_RANK, ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING;
		
		override fun toString(): String = when (this)
		{
			NO_RANK -> "?"
			ACE, JACK, QUEEN, KING -> name[0].toString()
			else -> ordinal.toString()
		}
	}
	
	enum class Suit
	{
		NO_SUIT, DIAMONDS, CLUBS, HEARTS, SPADES, JOKER;
		
		override fun toString(): String = when (this)
		{
			NO_SUIT -> "?"
			DIAMONDS -> "♢"
			CLUBS -> "♣"
			HEARTS -> "♡"
			SPADES -> "♠"
			JOKER -> "JOKER"
		}
	}
}
