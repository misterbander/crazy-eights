package misterbander.crazyeights.model

import ktx.collections.*
import kotlin.math.round

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
		card.rotation = 180*round((card.rotation - rotation)/180)
		card.cardGroupId = id
	}
	
	operator fun minusAssign(card: ServerCard)
	{
		cards -= card
		card.x = x
		card.y = y
		card.rotation += rotation
		card.cardGroupId = -1
	}
	
	override fun toString(): String =
		"ServerCardGroup(id=$id, x=$x, y=$y, rotation=$rotation, cards(${cards.size})=${
			cards.joinToString(prefix = "[", postfix = "]", limit = 10) { it.name }
		}, type=$type, lockholder=$lockHolder)"
	
	enum class Type
	{
		STACK, PILE, HAND
	}
}
