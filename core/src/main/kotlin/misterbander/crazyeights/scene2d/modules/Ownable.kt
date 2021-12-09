package misterbander.crazyeights.scene2d.modules

import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.Groupable
import misterbander.crazyeights.scene2d.MyHand
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec

class Ownable(
	private val room: Room,
	private val id: Int,
	draggable: Draggable
) : GModule<CrazyEights>(draggable.parent)
{
	val myHand: MyHand?
		get() = parent.firstAscendant(MyHand::class.java)
	val isOwned: Boolean
		get() = myHand != null
	var wasInHand = false
	
	@Suppress("UNCHECKED_CAST")
	fun updateOwnership(x: Float, y: Float)
	{
		val (screenX, screenY) = parent.localToScreenCoordinates(tempVec.set(x, y))
		val (_, uiStageY) = room.uiStage.screenToStageCoordinates(tempVec.set(screenX, screenY))
		
		if (isOwned)
		{
			if (uiStageY > 98 + room.tabletop.myHand.offsetCenterY) // Should disown it from hand?
			{
				val smoothMovable = parent.getModule<SmoothMovable>()!!
				room.tabletop.myHand -= parent as Groupable<CardGroup>
				room.tabletop.myHand.arrange()
				game.client?.apply {
					outgoingPacketBuffer += ObjectDisownEvent(
						id,
						smoothMovable.xInterpolator.target,
						smoothMovable.yInterpolator.target,
						smoothMovable.rotationInterpolator.target,
						if (parent is Card) parent.isFaceUp else false,
						game.user.name
					)
				}
			}
		}
		else
		{
			if (uiStageY <= 92 + room.tabletop.myHand.offsetCenterY) // Should own it in hand?
			{
				room.tabletop.myHand += parent as Groupable<CardGroup>
				room.tabletop.myHand.arrange()
				game.client?.apply { outgoingPacketBuffer += ObjectOwnEvent(id, game.user.name) }
			}
		}
	}
}
