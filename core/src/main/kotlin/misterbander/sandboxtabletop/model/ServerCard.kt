package misterbander.sandboxtabletop.model

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
	
	enum class Rank
	{
		NO_RANK, ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING;
		
		override fun toString(): String
		{
			if (this == NO_RANK)
				return ""
			return if (this == ACE || this == JACK || this == QUEEN || this == KING) super.toString().lowercase() else ordinal.toString()
		}
	}
	
	enum class Suit
	{
		NO_SUIT, DIAMONDS, CLUBS, HEARTS, SPADES, JOKER;
		
		override fun toString(): String = super.toString().lowercase()
	}
}
