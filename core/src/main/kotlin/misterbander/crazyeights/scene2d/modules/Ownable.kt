package misterbander.crazyeights.scene2d.modules

import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Hand
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec

class Ownable(
	private val room: Room,
	private val id: Int,
	draggable: Draggable
) : GModule<CrazyEights>(draggable.parent)
{
	val hand: Hand?
		get() = parent.parent as? Hand?
	val isOwned: Boolean
		get() = hand != null
	var wasInHand = false
	
	fun updateOwnership(x: Float, y: Float)
	{
		val (screenX, screenY) = parent.localToScreenCoordinates(tempVec.set(x, y))
		val (_, uiStageY) = room.uiStage.screenToStageCoordinates(tempVec.set(screenX, screenY))
		
		if (isOwned)
		{
			if (uiStageY > 95 + room.tabletop.hand.offsetCenterY) // Should disown it from hand?
			{
				val smoothMovable = parent.getModule<SmoothMovable>()!!
				room.tabletop.hand -= parent
				game.client?.apply {
					outgoingPacketBuffer += ObjectDisownEvent(
						id,
						smoothMovable.xInterpolator.target,
						smoothMovable.yInterpolator.target,
						smoothMovable.rotationInterpolator.target,
						if (parent is Card) parent.isFaceUp else false,
						game.user.username
					)
				}
			}
		}
		else
		{
			if (uiStageY <= 95 + room.tabletop.hand.offsetCenterY) // Should own it in hand?
			{
				room.tabletop.hand += parent
				game.client?.apply { outgoingPacketBuffer += ObjectOwnEvent(id, game.user.username) }
			}
		}
	}
}
