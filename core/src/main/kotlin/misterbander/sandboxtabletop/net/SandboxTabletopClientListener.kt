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
import misterbander.sandboxtabletop.net.packets.FlipCardEvent
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent
import misterbander.sandboxtabletop.scene2d.Card
import misterbander.sandboxtabletop.scene2d.Lockable
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
			is ObjectLockEvent -> Gdx.app.postRunnable { // User attempts to lock an object
				val (id, lockerUsername) = `object`
				val toLock = tabletop.idGObjectMap[id]!!
				val lockable = toLock.getModule<Lockable>()
				if (lockable != null && !lockable.isLocked)
				{
					toLock.toFront()
					lockable.lockHolder = tabletop.users[lockerUsername]
				}
			}
			is ObjectUnlockEvent -> Gdx.app.postRunnable {
				val toUnlock = tabletop.idGObjectMap[`object`.id]
				toUnlock.getModule<Lockable>()?.lockHolder = null
			}
			is ObjectMovedEvent ->
			{
				val gObject = tabletop.idGObjectMap[`object`.id]!!
				gObject.getModule<SmoothMovable>()?.setTargetPosition(`object`.x, `object`.y)
				objectMovedEventPool.free(`object`)
			}
			is FlipCardEvent -> Gdx.app.postRunnable {
				val card = tabletop.idGObjectMap[`object`.id] as Card
				card.isFaceUp = !card.isFaceUp
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
//	}
}