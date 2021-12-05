package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class Hand(private val room: Room) : GObject<CrazyEights>(room), DragTarget
{
	val offsetCenterY = 48F
	val cardGroup = CardGroup(room, y = offsetCenterY, type = ServerCardGroup.Type.SPREAD)
	
	init
	{
		addActor(cardGroup)
		room.addUprightGObject(this)
	}
	
	override fun update(delta: Float)
	{
		val (handX, handY) = stage.screenToStageCoordinates(tempVec.set(Gdx.graphics.width.toFloat()/2, Gdx.graphics.height.toFloat()))
		setPosition(handX, handY)
	}
	
	operator fun plusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup += groupable
	}
	
	operator fun minusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup -= groupable
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
			val insertIndex = cardGroup.cards.indexOf(gObject, true)
			cardGroup -= gObject
			val cards: Array<Card> = gObject.cards.toArray(Card::class.java)
			cards.forEachIndexed { index, card ->
				card.ownable.wasInHand = true
				card.isFaceUp = true
				cardGroup.insert(card, insertIndex + index)
			}
			arrange(false)
			gObject.remove()
			sendUpdates()
		}
	}
	
	fun arrange(sort: Boolean = true)
	{
		val defaultSeparation = 100F
		val max = room.uiViewport.worldWidth - 200
		cardGroup.spreadSeparation =
			if ((cardGroup.cards.size - 1)*defaultSeparation < max) defaultSeparation else max/(cardGroup.cards.size - 1)
		cardGroup.arrange(sort)
	}
	
	fun sendUpdates()
	{
		game.client?.apply {
			outgoingPacketBuffer += HandUpdateEvent(cardGroup.cards.map { (it as Card).toServerCard() }, game.user.username)
		}
	}
	
	fun reset() = cardGroup.clearChildren()
}
