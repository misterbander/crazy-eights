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
import ktx.collections.GdxArray
import ktx.collections.gdxArrayOf
import ktx.collections.isNotEmpty
import ktx.collections.plusAssign
import ktx.collections.set
import ktx.log.debug
import ktx.log.info
import misterbander.sandboxtabletop.VERSION_STRING
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.ServerCard.Rank
import misterbander.sandboxtabletop.model.ServerCard.Suit
import misterbander.sandboxtabletop.model.ServerCardGroup
import misterbander.sandboxtabletop.model.ServerLockable
import misterbander.sandboxtabletop.model.ServerObject
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.CardGroupChangedEvent
import misterbander.sandboxtabletop.net.packets.CardGroupCreatedEvent
import misterbander.sandboxtabletop.net.packets.CardGroupDismantledEvent
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
	val state = TabletopState()
	
	private val asyncContext = newSingleThreadAsyncContext("SandboxTabletopServer-AsyncExecutor-Thread")
	private val server = Server().apply {
		kryo.registerClasses()
		addListener(ServerListener())
	}
	@Volatile private var isStopped = false
	
	init
	{
		addServerObject(ServerCard(newId, x = 30F, y = 40F, rotation = 0F, rank = Rank.FIVE, suit = Suit.HEARTS, isFaceUp = true))
		addServerObject(ServerCard(newId, x = 30F, y = 40F, rotation = 30F, rank = Rank.TWO, suit = Suit.SPADES, isFaceUp = true))
		addServerObject(ServerCardGroup(
			newId, x = 640F, y = 360F, rotation = 0F, cards = gdxArrayOf(
				ServerCard(newId, suit = Suit.JOKER),
				ServerCard(newId, rank = Rank.KING, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.QUEEN, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.JACK, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.NINE, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.EIGHT, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.FIVE, suit = Suit.CLUBS),
				ServerCard(newId, rank = Rank.ACE, suit = Suit.CLUBS)
			)
		))
		addServerObject(ServerCardGroup(
			newId, x = 800F, y = 400F, rotation = 15F, cards = gdxArrayOf(
				ServerCard(newId, rank = Rank.KING, suit = Suit.HEARTS),
				ServerCard(newId, rank = Rank.QUEEN, suit = Suit.HEARTS)
			)
		))
		
		debug("Server | DEBUG") { "Initialized Room server" }
		debug("Server | DEBUG") { "ID object map = ${idToObjectMap.map { "\n\t$it" }}" }
		debug("Server | DEBUG") { "Server objects = ${state.serverObjects.map { "\n\t$it" }}" }
	}
	
	private fun addServerObject(serverObject: ServerObject, insertAtIndex: Int = -1)
	{
		idToObjectMap[serverObject.id] = serverObject
		if (serverObject is ServerCardGroup)
			serverObject.cards.forEach { idToObjectMap[it.id] = it }
		if (insertAtIndex != -1)
			state.serverObjects.insert(insertAtIndex, serverObject)
		else
			state.serverObjects += serverObject
	}
	
	fun start(port: Int)
	{
		server.start()
		server.bind(port)
	}
	
	private fun setServerCardGroup(id: Int, newCardGroupId: Int)
	{
		val card = idToObjectMap[id] as ServerCard
		val cardGroup = if (card.cardGroupId != -1) idToObjectMap[card.cardGroupId] as ServerCardGroup else null
		val newCardGroup = if (newCardGroupId != -1) idToObjectMap[newCardGroupId] as ServerCardGroup else null
		cardGroup?.minusAssign(card)
		if (newCardGroup != null)
		{
			newCardGroup += card
			state.serverObjects.removeValue(card, true)
		}
		else
			state.serverObjects += card
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
					if (toLock is ServerLockable && toLock.canLock) // Only unlocked draggables can be locked
					{
						debug("Server | DEBUG") { "$lockerUsername locks $toLock" }
						toLock.lockHolder = state.users[lockerUsername]
						// Remove the object and add it again to move it to the front
						if (state.serverObjects.removeValue(toLock, true))
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
						debug("Server | DEBUG") { "${toUnlock.lockHolder?.username} unlocks $toUnlock" }
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
				is CardGroupCreatedEvent ->
				{
					val cards = GdxArray<ServerCard>()
					`object`.cardIds.forEach { cards += idToObjectMap[it] as ServerCard }
					
					val (_, firstX, firstY, firstRotation) = cards[0]
					val cardGroup = ServerCardGroup(newId, firstX, firstY, firstRotation, cards)
					addServerObject(cardGroup, state.serverObjects.indexOf(cards[0], true))
					cards.forEach {
						it.x = 0F; it.y = 0F; it.rotation = 0F
						state.serverObjects.removeValue(it, true)
					}
					
					server.sendToAllTCP(`object`.copy(id = cardGroup.id))
				}
				is CardGroupChangedEvent ->
				{
					val (cardIds, newCardGroupId) = `object`
					cardIds.forEach { setServerCardGroup(it, newCardGroupId) }
					server.sendToAllTCP(`object`)
				}
				is CardGroupDismantledEvent ->
				{
					val cardGroup = idToObjectMap[`object`.id] as ServerCardGroup
					while (cardGroup.cards.isNotEmpty())
					{
						val card = cardGroup.cards.removeIndex(0)
						setServerCardGroup(card.id, -1)
					}
					state.serverObjects.removeValue(cardGroup, true)
					server.sendToAllTCP(`object`)
				}
			}
		}
	}
}
