package misterbander.sandboxtabletop.net

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.plusAssign
import ktx.collections.set
import ktx.log.info
import misterbander.sandboxtabletop.VERSION_STRING
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.ServerCard.Rank
import misterbander.sandboxtabletop.model.ServerCard.Suit
import misterbander.sandboxtabletop.model.ServerLockable
import misterbander.sandboxtabletop.model.ServerObject
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.FlipCardEvent
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectRotatedEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent

class SandboxTabletopServer
{
	private var newId = 0
		get() = field++
	private val idToObjectMap = IntMap<ServerObject>()
	private val state = TabletopState()
	
	private val asyncContext = newSingleThreadAsyncContext("SandboxTabletopServer-AsyncExecutor-Thread")
	private val server = Server().apply {
		kryo.registerClasses()
		addListener(ServerListener())
	}
	@Volatile private var isStopped = false
	
	init
	{
		addServerObject(ServerCard(newId, 30F, 40F, 0F, Rank.FIVE, Suit.HEARTS, true))
		addServerObject(ServerCard(newId, 30F, 40F, 30F, Rank.TWO, Suit.SPADES, true))
	}
	
	private fun addServerObject(serverObject: ServerObject)
	{
		idToObjectMap[serverObject.id] = serverObject
		state.serverObjects += serverObject
	}
	
	fun start(port: Int)
	{
		server.start()
		server.bind(port)
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stopAsync(): Deferred<Unit> = KtxAsync.async(asyncContext) {
		if (!isStopped)
		{
			server.stop()
			server.dispose()
			isStopped = true
		}
	}
	
	private inner class ServerListener : Listener
	{
		/** Contains ids of connections that have successfully performed a handshake. */
		private val handshookConnections = IntSet()
		
		override fun connected(connection: Connection) = connection.setName("Server-side client connection ${connection.id}")
		
		override fun disconnected(connection: Connection)
		{
			handshookConnections.remove(connection.id)
			if (connection.arbitraryData is User)
			{
				val user = connection.arbitraryData as User
				state.users.remove(user.username)
				state.serverObjects.forEach { serverObject: ServerObject ->
					if (serverObject is ServerLockable && serverObject.lockHolder == user)
						serverObject.lockHolder = null
				}
				server.sendToAllTCP(UserLeaveEvent(user))
				info("Server | INFO") { "${user.username} left the game" }
			}
		}
		
		override fun received(connection: Connection, `object`: Any)
		{
			if (connection.id !in handshookConnections) // Connections must perform handshake before packets are processed
			{
				if (`object` is Handshake)
				{
					if (`object`.versionString != VERSION_STRING) // Version check
					{
						connection.sendTCP(HandshakeReject("Incorrect version! Your Sandbox Tabletop version is ${`object`.versionString}. Server version is $VERSION_STRING."))
						return
					}
					if (`object`.data?.size != 1) // Data integrity check
					{
						connection.sendTCP(HandshakeReject("Incorrect handshake data format! Expecting 1 argument but found ${`object`.data?.size}. This is a bug and shouldn't be happening, please notify the developer."))
						return
					}
					val username = `object`.data[0]
					if (state.users[username] != null) // Check username collision
					{
						connection.sendTCP(HandshakeReject("Username conflict! Username $username is already taken."))
						return
					}
					
					// Handshake is successful
					handshookConnections.add(connection.id)
					connection.sendTCP(Handshake())
					info("Server | INFO") { "Successful handshake from $connection" }
				}
				else
					ktx.log.error("Server | ERROR") { "$connection attempted to send objects before handshake" }
				return
			}
			
			when (`object`)
			{
				is User -> // A new user tries to join
				{
					connection.arbitraryData = `object`
					state.users[`object`.username] = `object`
					connection.sendTCP(state)
					server.sendToAllTCP(UserJoinEvent(`object`))
					info("Server | INFO") { "${`object`.username} joined the game" }
				}
				is Chat -> server.sendToAllTCP(`object`)
				is CursorPosition ->
				{
					server.sendToAllTCP(`object`)
					cursorPositionPool.free(`object`)
				}
				is ObjectLockEvent -> // User attempts to lock an object
				{
					val (id, lockerUsername) = `object`
					val toLock = idToObjectMap[id]!!
					if (toLock is ServerLockable && !toLock.isLocked) // Only unlocked draggables can be locked
					{
						toLock.lockHolder = state.users[lockerUsername]
						state.serverObjects.removeValue(toLock, true)
						state.serverObjects += toLock
						server.sendToAllTCP(`object`)
					}
				}
				is ObjectUnlockEvent -> // User unlocks an object
				{
					val (id, unlockerUsername) = `object`
					val toUnlock = idToObjectMap[id]!!
					if (toUnlock is ServerLockable && toUnlock.lockHolder == state.users[unlockerUsername])
					{
						toUnlock.lockHolder = null
						server.sendToAllTCP(`object`)
					}
				}
				is ObjectMovedEvent ->
				{
					val (id, x, y) = `object`
					idToObjectMap[id].apply { this.x = x; this.y = y }
					server.sendToAllTCP(`object`)
					objectMovedEventPool.free(`object`)
				}
				is ObjectRotatedEvent ->
				{
					val (id, rotation) = `object`
					idToObjectMap[id].rotation = rotation
					server.sendToAllTCP(`object`)
					objectRotatedEventPool.free(`object`)
				}
				is FlipCardEvent ->
				{
					val card = idToObjectMap[`object`.id] as ServerCard
					card.isFaceUp = !card.isFaceUp
					server.sendToAllTCP(`object`)
				}
			}
		}
	}
}
