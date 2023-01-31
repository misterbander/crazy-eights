package misterbander.crazyeights.scene2d.modules

import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.Groupable
import misterbander.crazyeights.scene2d.MyHand
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec

class Ownable(
	private val parent: GObject<CrazyEights>,
	private val room: RoomScreen,
	private val id: Int
) : GModule
{
	private val game: CrazyEights
		get() = parent.game
	private val tabletop: Tabletop
		get() = room.tabletop
	val myHand: MyHand?
		get() = parent.firstAscendant(MyHand::class.java)
	val isOwned: Boolean
		get() = myHand != null
	var justAcceptedInHand = false
	
	@Suppress("UNCHECKED_CAST")
	fun updateOwnership(x: Float, y: Float)
	{
		val (screenX, screenY) = parent.localToScreenCoordinates(tempVec.set(x, y))
		val (_, uiStageY) = room.uiStage.screenToStageCoordinates(tempVec.set(screenX, screenY))
		
		val shouldOwn = uiStageY <= 92 + tabletop.myHand.offsetCenterY
		val shouldDisown = uiStageY > 98 + tabletop.myHand.offsetCenterY
		if (isOwned)
		{
			if (shouldDisown)
			{
				val smoothMovable = parent.getModule<SmoothMovable>()!!
				tabletop.myHand -= parent as Groupable<CardGroup>
				tabletop.myHand.arrange()
				game.client?.apply {
					outgoingPacketBuffer += ObjectDisownEvent(
						id,
						smoothMovable.x,
						smoothMovable.y,
						smoothMovable.rotation,
						if (parent is Card) parent.isFaceUp else false,
						game.user.name
					)
				}
			}
		}
		else if (shouldOwn)
		{
			tabletop.myHand += parent as Groupable<CardGroup>
			tabletop.myHand.arrange()
			game.client?.apply { outgoingPacketBuffer += ObjectOwnEvent(id, game.user.name) }
		}
	}
}
