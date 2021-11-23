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
		arrange()
	}
	
	operator fun plusAssign(card: ServerCard)
	{
		cards += card
		card.x -= x
		card.y -= y
		card.rotation -= rotation
		card.cardGroupId = id
	}
	
	operator fun minusAssign(card: ServerCard)
	{
		cards -= card
		card.x += x
		card.y += y
		card.rotation += rotation
		card.cardGroupId = -1
	}
	
	fun arrange()
	{
		if (type == Type.STACK)
		{
			cards.forEachIndexed { index, serverCard ->
				serverCard.x = -index.toFloat()
				serverCard.y = index.toFloat()
				serverCard.rotation = 180*round(serverCard.rotation/180)
			}
		}
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
