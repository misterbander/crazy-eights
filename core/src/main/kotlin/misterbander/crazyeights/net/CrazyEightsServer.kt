package misterbander.crazyeights.net

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.VERSION_STRING
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent

class CrazyEightsServer
{
	private var newId = 0
		get() = field++
	private val idToObjectMap = IntMap<ServerObject>()
	val state = TabletopState()
	
	private val asyncContext = newSingleThreadAsyncContext("CrazyEightsServer-AsyncExecutor-Thread")
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
				for (serverObject: ServerObject in state.serverObjects)
				{
					if (serverObject is ServerLockable && serverObject.lockHolder == user)
						serverObject.lockHolder = null
				}
				server.sendToAllTCP(UserLeftEvent(user))
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
						connection.sendTCP(HandshakeReject("Incorrect version! Your Crazy Eights version is ${`object`.versionString}. Server version is $VERSION_STRING."))
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
					server.sendToAllTCP(UserJoinedEvent(`object`))
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
				is ObjectMoveEvent ->
				{
					val (id, x, y) = `object`
					idToObjectMap[id].apply { this.x = x; this.y = y }
					server.sendToAllExceptTCP(connection.id, `object`)
					objectMoveEventPool.free(`object`)
				}
				is ObjectRotateEvent ->
				{
					val (id, rotation) = `object`
					idToObjectMap[id].rotation = rotation
					server.sendToAllExceptTCP(connection.id, `object`)
					objectRotateEventPool.free(`object`)
				}
				is CardFlipEvent ->
				{
					val card = idToObjectMap[`object`.id] as ServerCard
					card.isFaceUp = !card.isFaceUp
					server.sendToAllTCP(`object`)
				}
				is CardGroupCreateEvent ->
				{
					val cards = GdxArray<ServerCard>()
					`object`.cardIds.forEach { cards += idToObjectMap[it] as ServerCard }
					
					val (_, firstX, firstY, firstRotation) = cards[0]
					val cardGroup = ServerCardGroup(newId, firstX, firstY, firstRotation, cards)
					addServerObject(cardGroup, state.serverObjects.indexOf(cards[0], true))
					for (card: ServerCard in cards)
					{
						card.x = 0F; card.y = 0F; card.rotation = 0F
						state.serverObjects.removeValue(card, true)
					}
					
					server.sendToAllTCP(`object`.copy(id = cardGroup.id))
				}
				is CardGroupChangeEvent ->
				{
					val (cardIds, newCardGroupId) = `object`
					cardIds.forEach { setServerCardGroup(it, newCardGroupId) }
					server.sendToAllTCP(`object`)
				}
				is CardGroupDismantleEvent ->
				{
					val cardGroup = idToObjectMap[`object`.id] as ServerCardGroup
					while (cardGroup.cards.isNotEmpty())
					{
						val card = cardGroup.cards.removeIndex(0)
						setServerCardGroup(card.id, -1)
					}
					state.serverObjects.removeValue(cardGroup, true)
					server.sendToAllExceptTCP(connection.id, `object`)
				}
			}
		}
	}
}
