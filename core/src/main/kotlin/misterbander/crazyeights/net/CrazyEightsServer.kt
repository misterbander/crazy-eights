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
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent

class CrazyEightsServer
{
	private var maxId = 0
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
		val deck = GdxArray<ServerCard>()
		for (suit in Suit.values())
		{
			if (suit == Suit.NO_SUIT || suit == Suit.JOKER)
				continue
			for (rank in Rank.values())
			{
				if (rank != Rank.NO_RANK)
					deck += ServerCard(newId(), rank = rank, suit = suit)
			}
		}
		addServerObject(ServerCardHolder(newId(), x = 540F, y = 360F, cardGroup = ServerCardGroup(newId(), cards = deck)))
		addServerObject(ServerCardHolder(newId(), x = 740F, y = 360F, cardGroup = ServerCardGroup(newId(), type = ServerCardGroup.Type.PILE)))
		
		debug("Server | DEBUG") { "Initialized Room server" }
	}
	
	private fun newId(): Int = maxId++
	
	private fun addServerObject(serverObject: ServerObject, insertAtIndex: Int = -1)
	{
		idToObjectMap[serverObject.id] = serverObject
		if (serverObject is ServerCardGroup)
			serverObject.cards.forEach { idToObjectMap[it.id] = it }
		else if (serverObject is ServerCardHolder)
		{
			idToObjectMap[serverObject.cardGroup.id] = serverObject.cardGroup
			serverObject.cardGroup.cards.forEach { idToObjectMap[it.id] = it }
		}
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
	
	private fun ServerCard.setServerCardGroup(newCardGroup: ServerCardGroup?)
	{
		val cardGroup = if (cardGroupId != -1) idToObjectMap[cardGroupId] as ServerCardGroup else null
		cardGroup?.minusAssign(this)
		if (newCardGroup != null)
		{
			newCardGroup += this
			state.serverObjects.removeValue(this, true)
		}
		else
			state.serverObjects += this
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
						if (toUnlock is ServerCard)
						{
							if (toUnlock.cardGroupId != -1)
								(idToObjectMap[toUnlock.cardGroupId] as ServerCardGroup).arrange()
						}
						else if (toUnlock is ServerCardGroup)
						{
							if (toUnlock.cardHolder != null)
								toUnlock.rotation = 0F
						}
						server.sendToAllTCP(`object`)
					}
				}
				is ObjectOwnEvent ->
				{
					val (id, ownerUsername) = `object`
					val toOwn = idToObjectMap[id]!!
					state.serverObjects -= toOwn
					state.hands.getOrPut(ownerUsername) { GdxArray() } += toOwn
					server.sendToAllExceptTCP(connection.id, `object`)
				}
				is ObjectDisownEvent ->
				{
					val (id, x, y, rotation, isFaceUp, disownerUsername) = `object`
					val toDisown = idToObjectMap[id]!!
					toDisown.x = x
					toDisown.y = y
					toDisown.rotation = rotation
					if (toDisown is ServerLockable)
						toDisown.lockHolder = state.users[disownerUsername]
					if (toDisown is ServerCard)
						toDisown.isFaceUp = isFaceUp
					state.serverObjects += toDisown
					state.hands[disownerUsername].removeValue(toDisown, true)
					server.sendToAllExceptTCP(connection.id, `object`)
				}
				is HandUpdateEvent ->
				{
					val (hand, ownerUsername) = `object`
					state.hands[ownerUsername] = hand
					hand.forEach { idToObjectMap[it.id] = it }
					server.sendToAllExceptTCP(connection.id, `object`)
				}
				is ObjectMoveEvent ->
				{
					val (id, x, y) = `object`
					val toMove = idToObjectMap[id]!!
					toMove.x = x
					toMove.y = y
					if (toMove is ServerCardGroup && toMove.isLocked && toMove.type == ServerCardGroup.Type.PILE)
						toMove.type = ServerCardGroup.Type.STACK
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
					val cards = `object`.cards
					val (firstId, firstX, firstY, firstRotation) = cards[0]
					val cardGroup = ServerCardGroup(newId(), firstX, firstY, firstRotation)
					val insertAtIndex = state.serverObjects.indexOfFirst { it.id == firstId }
					cards.forEachIndexed { index, (id, x, y, rotation) ->
						val card = idToObjectMap[id] as ServerCard
						state.serverObjects.removeValue(card, true)
						card.x = x
						card.y = y
						card.rotation = rotation
						cards[index] = card
						cardGroup += card
					}
					cardGroup.arrange()
					addServerObject(cardGroup, insertAtIndex)
					server.sendToAllTCP(`object`.copy(id = cardGroup.id))
				}
				is CardGroupChangeEvent ->
				{
					val (cards, newCardGroupId) = `object`
					val newCardGroup = if (newCardGroupId != -1) idToObjectMap[newCardGroupId] as ServerCardGroup else null
					cards.forEachIndexed { index, (id, _, _, rotation) ->
						val card = idToObjectMap[id] as ServerCard
						card.rotation = rotation
						card.setServerCardGroup(newCardGroup)
						cards[index] = card
					}
					newCardGroup?.arrange()
					server.sendToAllTCP(`object`)
				}
				is CardGroupDetachEvent ->
				{
					val (cardHolderId, _, changerUsername) = `object`
					val cardHolder = idToObjectMap[cardHolderId] as ServerCardHolder
					val cardGroup = cardHolder.cardGroup
					cardGroup.x = cardHolder.x
					cardGroup.y = cardHolder.y
					cardGroup.rotation = cardHolder.rotation
					cardGroup.cardHolder = null
					state.serverObjects += cardGroup
					val replacementCardGroup = ServerCardGroup(newId(), type = cardHolder.defaultType)
					idToObjectMap[replacementCardGroup.id] = replacementCardGroup
					cardHolder.cardGroup = replacementCardGroup
					replacementCardGroup.cardHolder = cardHolder
					server.sendToAllTCP(CardGroupDetachEvent(cardHolderId, cardHolder.cardGroup.id, changerUsername))
				}
				is CardGroupDismantleEvent ->
				{
					val cardGroup = idToObjectMap[`object`.id] as ServerCardGroup
					while (cardGroup.cards.isNotEmpty())
					{
						val card: ServerCard = cardGroup.cards.removeIndex(0)
						card.setServerCardGroup(null)
					}
					state.serverObjects.removeValue(cardGroup, true)
					server.sendToAllExceptTCP(connection.id, `object`)
				}
			}
			
			state.updateDebugStrings()
		}
	}
}
