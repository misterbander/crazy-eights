package misterbander.crazyeights.net.server

import com.badlogic.gdx.math.MathUtils
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.scene2d.getSpreadPositionForIndex
import misterbander.crazyeights.scene2d.getSpreadRotationForIndex
import misterbander.gframework.util.shuffle
import kotlin.math.round

data class ServerCardGroup(
	override val id: Int = -1,
	override var x: Float = 0F,
	override var y: Float = 0F,
	override var rotation: Float = 0F,
	var spreadSeparation: Float = 40F,
	var spreadCurvature: Float = 0.07F,
	val cards: GdxArray<ServerCard> = GdxArray(),
	var type: Type = Type.STACK,
	override var lockHolder: String? = null,
	var cardHolderId: Int = -1
) : ServerLockable, ServerOwnable
{
	override val canLock: Boolean
		get() = !isLocked && (cards.isEmpty || !cards.peek().isLocked)
	
	init
	{
		cards.forEach { it.cardGroupId = id }
		arrange()
	}
	
	fun addCard(tabletop: ServerTabletop, card: ServerCard)
	{
		cards += card
		if (type == Type.PILE)
		{
			val cardHolder = if (cardHolderId != -1) tabletop.findObjectById<ServerCardHolder>(cardHolderId) else null
			if (cards.size == 1)
			{
				card.x = 0F
				card.y = 0F
				card.rotation = 180*round((card.rotation - rotation - (cardHolder?.rotation ?: 0F))/180)
			}
			else
			{
				card.x = MathUtils.random(-10F, 10F)
				card.y = MathUtils.random(-10F, 10F)
				card.rotation -= rotation + (cardHolder?.rotation ?: 0F) + MathUtils.random(-30F, 30F)
			}
		}
		else
		{
			card.x -= x
			card.y -= y
			card.rotation -= rotation
		}
		card.cardGroupId = id
	}
	
	fun removeCard(tabletop: ServerTabletop, card: ServerCard)
	{
		val cardHolder = if (cardHolderId != -1) tabletop.findObjectById<ServerCardHolder>(cardHolderId) else null
		cards -= card
		card.x += x + (cardHolder?.x ?: 0F)
		card.y += y + (cardHolder?.y ?: 0F)
		card.rotation += rotation + (cardHolder?.rotation ?: 0F)
		card.cardGroupId = -1
	}
	
	fun arrange()
	{
		if (type == Type.STACK)
		{
			cards.forEachIndexed { index, card: ServerCard ->
				card.x = -index.toFloat()
				card.y = index.toFloat()
				card.rotation = 180*round(card.rotation/180)
			}
		}
		else if (type == Type.SPREAD)
		{
			cards.forEachIndexed { index, card: ServerCard ->
				val (x, y) = getSpreadPositionForIndex(index, cards.size, spreadSeparation, spreadCurvature)
				val rotation = getSpreadRotationForIndex(index, cards.size, spreadSeparation, spreadCurvature)
				card.x = x
				card.y = y
				card.rotation = rotation
			}
		}
	}
	
	fun shuffle(tabletop: ServerTabletop, seed: Long)
	{
		val cardHolder = if (cardHolderId != -1) tabletop.findObjectById<ServerCardHolder>(cardHolderId) else null
		cardHolder?.toFront(tabletop) ?: toFront(tabletop)
		cards.shuffle(seed)
	}
	
	override fun toString(): String =
		"ServerCardGroup(id=$id, x=$x, y=$y, rotation=$rotation, cards(${cards.size})=${
			cards.joinToString(prefix = "[", postfix = "]", limit = 10) { it.name }
		}, type=$type, lockholder=$lockHolder)"
	
	enum class Type
	{
		STACK, PILE, SPREAD
	}
}
