package misterbander.sandboxtabletop.model

import ktx.collections.GdxArray
import ktx.collections.minusAssign
import ktx.collections.plusAssign

data class ServerCardGroup(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	val cards: GdxArray<ServerCard> = GdxArray(),
	val type: Type = Type.STACK,
	override var lockHolder: User? = null
) : ServerObject, ServerLockable
{
	override val canLock: Boolean
		get() = !isLocked && (cards.isEmpty || !cards.peek().isLocked)
	
	init
	{
		cards.forEach { it.cardGroupId = id }
	}
	
	operator fun plusAssign(card: ServerCard)
	{
		cards += card
		card.x = 0F
		card.y = 0F
		card.rotation = 0F
		card.cardGroupId = id
	}
	
	operator fun minusAssign(card: ServerCard)
	{
		cards -= card
		card.x = x
		card.y = y
		card.rotation = rotation
		card.cardGroupId = -1
	}
	
	override fun toString(): String = "ServerCardGroup(id=$id, x=$x, y=$y, rotation=$rotation, cards=(${cards.size}), type=$type, lockholder=$lockHolder)"
	
	enum class Type
	{
		STACK, PILE, HAND
	}
}
