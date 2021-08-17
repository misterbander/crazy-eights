package misterbander.sandboxtabletop.net

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.log.info
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.net.packets.LockEvent
import misterbander.sandboxtabletop.net.packets.ServerObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent
import misterbander.sandboxtabletop.scene2d.Draggable
import misterbander.sandboxtabletop.scene2d.SandboxTabletopCursor
import misterbander.sandboxtabletop.scene2d.SmoothMovable

class SandboxTabletopClientListener(private val screen: RoomScreen): Listener
{
	private val game = screen.game
	
	private val tabletop = screen.tabletop
	
	override fun disconnected(connection: Connection)
	{
		val menuScreen = game.getScreen<MenuScreen>()
		if (!screen.selfDisconnect)
			menuScreen.messageDialog.show("Disconnected", "Server closed.", "OK")
		screen.transition.start(targetScreen = menuScreen)
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		when (`object`)
		{
			is UserJoinEvent -> Gdx.app.postRunnable {
				val user = `object`.user
				if (user != game.user)
					tabletop += user
				screen.chat("${user.username} joined the game", Color.YELLOW)
			}
			is UserLeaveEvent -> Gdx.app.postRunnable {
				val user = `object`.user
				tabletop -= user
				screen.chat("${user.username} left the game", Color.YELLOW)
			}
			is Chat -> Gdx.app.postRunnable {
				screen.chat(`object`.message, if (`object`.isSystemMessage) Color.YELLOW else null)
				info("Client | CHAT") { `object`.message }
			}
			is CursorPosition ->
			{
				val cursor: SandboxTabletopCursor? = tabletop.userCursorMap[`object`.username]
				cursor?.setTargetPosition(`object`.x, `object`.y)
				cursorPositionPool.free(`object`)
			}
			is LockEvent -> Gdx.app.postRunnable {
				val gObject = tabletop.idGObjectMap[`object`.serverObjectId]!!
				val draggable = gObject.getModule<Draggable>()
				if (draggable != null)
				{
					gObject.toFront()
					draggable.lockHolder = if (`object`.lockerUsername != null) tabletop.users[`object`.lockerUsername] else null
				}
			}
			is ServerObjectMovedEvent ->
			{
				val gObject = tabletop.idGObjectMap[`object`.id]!!
				gObject.getModule<SmoothMovable>()?.setTargetPosition(`object`.x, `object`.y)
				serverObjectMovedEventPool.free(`object`)
			}
		}
	}

//	fun objectReceived(connection: Connection?, `object`: Serializable)
//	{
//		if (`object` is OwnerEvent)
//		{
//			val event: OwnerEvent = `object` as OwnerEvent
//			val actor: Actor = uuidActorMap.get<UUID>(event.ownedUuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				card.owner = event.owner
//				card.setVisible(card.owner == null || card.owner.equals(user))
//			}
//		}
//		else if (`object` is FlipCardEvent)
//		{
//			val flipCardEvent: FlipCardEvent = `object` as FlipCardEvent
//			val actor: Actor = uuidActorMap.get<UUID>(flipCardEvent.uuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				card.setFaceUp(flipCardEvent.isFaceUp)
//				if (card.owner == null) card.setZIndex(uuidActorMap.size)
//			}
//		}
//	}
}