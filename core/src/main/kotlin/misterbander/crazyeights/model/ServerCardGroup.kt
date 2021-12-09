package misterbander.crazyeights.model

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
) : ServerObject, ServerLockable
{
	override val canLock: Boolean
		get() = !isLocked && (cards.isEmpty || !cards.peek().isLocked)
	
	init
	{
		cards.forEach { it.cardGroupId = id }
		arrange()
	}
	
	fun plusAssign(card: ServerCard, state: TabletopState)
	{
		cards += card
		if (type == Type.PILE)
		{
			val cardHolder = state.idToObjectMap[cardHolderId] as? ServerCardHolder
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
	
	fun minusAssign(card: ServerCard, state: TabletopState)
	{
		val cardHolder = state.idToObjectMap[cardHolderId] as? ServerCardHolder
		cards -= card
		card.x += x
		card.y += y
		card.rotation += rotation + (cardHolder?.rotation ?: 0F)
		card.cardGroupId = -1
	}
	
	fun arrange()
	{
		if (type == Type.STACK)
		{
			cards.forEachIndexed { index, serverCard: ServerCard ->
				serverCard.x = -index.toFloat()
				serverCard.y = index.toFloat()
				serverCard.rotation = 180*round(serverCard.rotation/180)
			}
		}
		else if (type == Type.SPREAD)
		{
			cards.forEachIndexed { index, serverCard: ServerCard ->
				val (x, y) = getSpreadPositionForIndex(index, cards.size, spreadSeparation, spreadCurvature)
				val rotation = getSpreadRotationForIndex(index, cards.size, spreadSeparation, spreadCurvature)
				serverCard.x = x
				serverCard.y = y
				serverCard.rotation = rotation
			}
		}
	}
	
	fun shuffle(seed: Long, state: TabletopState)
	{
		val cardHolder = state.idToObjectMap[cardHolderId] as? ServerCardHolder
		cardHolder?.toFront(state) ?: toFront(state)
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
