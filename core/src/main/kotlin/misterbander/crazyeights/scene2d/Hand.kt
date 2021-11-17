package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class Hand(
	private val room: Room,
	private val ownables: GdxArray<GObject<CrazyEights>>
) : GObject<CrazyEights>(room), DragTarget
{
	private val ownableGhostMap = GdxMap<GObject<CrazyEights>, CardGhost>()
	private val comparator = Comparator<GObject<CrazyEights>> { o1, o2 -> if (o1.x == o2.x) 0 else if (o1.x > o2.x) 1 else -1 }
	private val separation: Float
		get()
		{
			val defaultSeparation = 100F
			val max = room.uiViewport.worldWidth - 200
			return if ((ownables.size - 1)*defaultSeparation < max) defaultSeparation else max/(ownables.size - 1)
		}
	private val curvature: Float
		get() = 0.07F
	val offsetCenterY = 48F
	
	init
	{
		for (ownable: GObject<CrazyEights> in ownables)
		{
			addActor(ownable)
			val ghost = CardGhost(ownable)
			ownableGhostMap[ownable] = ghost
			this += ghost
		}
	}
	
	fun reposition(screenWidth: Int = Gdx.graphics.width, screenHeight: Int = Gdx.graphics.height)
	{
		val (handX, handY) = stage.screenToStageCoordinates(tempVec.set(screenWidth.toFloat()/2, screenHeight.toFloat()))
		setPosition(handX, handY + offsetCenterY)
	}
	
	operator fun plusAssign(ownable: GObject<CrazyEights>)
	{
		ownable.transformToGroupCoordinates(this)
		ownables += ownable
		addActor(ownable)
		val ghost = CardGhost(ownable)
		ownableGhostMap[ownable] = ghost
		addActor(ghost)
		arrange()
	}
	
	operator fun minusAssign(ownable: GObject<CrazyEights>)
	{
		ownable.transformToGroupCoordinates(room.tabletop.cards)
		ownables -= ownable
		room.tabletop.cards.addActor(ownable)
		removeActor(ownableGhostMap.remove(ownable))
		arrange()
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean = gObject is Card || gObject is CardGroup
	
	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
		{
			arrange()
			if (!gObject.ownable.wasInHand)
			{
				gObject.ownable.wasInHand = true
				gObject.isFaceUp = true
				sendUpdates()
			}
		}
		else if (gObject is CardGroup)
		{
			val insertIndex = ownables.indexOf(gObject, true)
			this -= gObject
			val cards: Array<Card> = gObject.children.toArray(Card::class.java)
			for (i in cards.indices)
			{
				val card = cards[i]
				card.transformToGroupCoordinates(this)
				card.ownable.wasInHand = true
				card.isFaceUp = true
				ownables.insert(insertIndex + i, card)
				addActor(card)
				val ghost = CardGhost(card)
				ownableGhostMap[card] = ghost
				addActor(ghost)
			}
			arrange(false)
			gObject.remove()
			sendUpdates()
		}
	}
	
	fun arrange(sort: Boolean = true)
	{
		if (sort)
			ownables.sort(comparator)
		for (i in 0 until ownables.size)
		{
			val ownable: GObject<CrazyEights> = ownables[i]
			val ghost = ownableGhostMap[ownable]!!
			val (x, y) = getCardPositionForIndex(i)
			val rotation = getCardRotationForIndex(i)
			if (!ownable.getModule<Lockable>()!!.isLocked)
			{
				ownable.getModule<SmoothMovable>()!!.apply {
					xInterpolator.smoothingFactor = 5F
					yInterpolator.smoothingFactor = 5F
					setTargetPosition(x, y)
					rotationInterpolator.target = rotation
				}
				ownable.zIndex = 2*i + 1
			}
			ghost.setPosition(x, y)
			ghost.rotation = rotation
			ghost.zIndex = 2*i
		}
	}
	
	private fun getCardPositionForIndex(index: Int): Vector2
	{
		val offsetFactor = -(ownables.size - 1)/2F + index
		return tempVec.set(offsetFactor*separation, -offsetFactor*offsetFactor*curvature)
	}
	
	private fun getCardRotationForIndex(index: Int): Float = -getCardPositionForIndex(index).x/25
	
	fun sendUpdates()
	{
		val hand: GdxArray<ServerObject> = ownables.map { (it as Card).toServerCard() }
		game.client?.sendTCP(HandUpdateEvent(hand, game.user.username))
	}
	
	fun reset()
	{
		ownables.clear()
		ownableGhostMap.clear()
		clearChildren()
	}
}
