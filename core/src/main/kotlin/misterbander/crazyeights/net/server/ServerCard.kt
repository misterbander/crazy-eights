package misterbander.crazyeights.net.server

import ktx.collections.*

data class ServerCard(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	val rank: Rank = Rank.NO_RANK,
	val suit: Suit = Suit.NO_SUIT,
	var isFaceUp: Boolean = false,
	override var lockHolder: String? = null,
	var lastOwner: String? = null,
	var cardGroupId: Int = -1,
	var justMoved: Boolean = false,
	var justRotated: Boolean = false
) : ServerLockable, ServerOwnable
{
	val name: String
		get() = if (suit == Suit.JOKER) "JOKER" else "$rank$suit"
	
	private fun getServerCardGroup(tabletop: ServerTabletop): ServerCardGroup? =
		if (cardGroupId != -1) tabletop.findObjectById(cardGroupId) else null
	
	fun setServerCardGroup(tabletop: ServerTabletop, cardGroup: ServerCardGroup?)
	{
		getServerCardGroup(tabletop)?.removeCard(tabletop, this)
		if (cardGroup != null)
		{
			cardGroup.addCard(tabletop, this)
			tabletop.serverObjects.removeValue(this, true)
		}
		else
			tabletop.serverObjects += this
	}
	
	override fun setOwner(tabletop: ServerTabletop, ownerUsername: String)
	{
		if (cardGroupId != -1)
			tabletop.findObjectById<ServerCardGroup>(cardGroupId).removeCard(tabletop, this)
		lastOwner = ownerUsername
		super.setOwner(tabletop, ownerUsername)
	}
	
	override fun toString(): String =
		"ServerCard($name, id=$id, x=$x, y=$y, rotation=$rotation, isFaceUp=$isFaceUp, lockHolder=$lockHolder)"
	
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
