package misterbander.crazyeights.net

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.VERSION_STRING
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.ai.IsmctsAgent
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
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.NewGameActionFinishedEvent
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent

class CrazyEightsServer
{
	private var maxId = 0
	val state = TabletopState()
	// Some actions play a client-side animation which takes some time. While the animation is playing, we must ensure
	// that no other events take place to prevent events from overlapping, causing strange behavior. This is achieved
	// using action locks.
	// If an event that plays a client-side animation occurs, each currently online user will obtain an action lock.
	// Action locks will only be released once the client-side animation finishes, or the user leaves the room.
	private val actionLocks = GdxSet<String>()
	private val aiNames = gdxArrayOf("Shark (AI)", "Queenpin (AI)", "Watson (AI)", "Ning (AI)")
	private var aiCount = 0
	
	private val asyncContext = newSingleThreadAsyncContext("CrazyEightsServer-AsyncExecutor-Thread")
	private val server by lazy {
		Server().apply {
			kryo.registerClasses()
			addListener(ServerListener())
		}
	}
	private val startServerJob = Job()
	@Volatile private var isStopped = false
	
	private var serverGameState: ServerGameState? = null
	private val isGameStarted: Boolean
		get() = serverGameState != null
	
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
		val drawStackHolder = ServerCardHolder(newId(), x = 540F, y = 360F, cardGroup = ServerCardGroup(newId(), cards = deck))
		val discardPileHolder = ServerCardHolder(newId(), x = 740F, y = 360F, cardGroup = ServerCardGroup(newId(), type = ServerCardGroup.Type.PILE))
		state.drawStackHolderId = drawStackHolder.id
		state.discardPileHolderId = discardPileHolder.id
		state.addServerObject(drawStackHolder)
		state.addServerObject(discardPileHolder)
		
		debug("Server | DEBUG") { "Initialized Room server" }
	}
	
	private fun newId(): Int = maxId++
	
	fun start(port: Int)
	{
		try
		{
			server.start()
			server.bind(port)
		}
		finally
		{
			startServerJob.complete()
		}
	}
	
	fun ServerCardGroup.draw(ownerUsername: String)
	{
		val card: ServerCard = cards.peek()
		card.isFaceUp = true
		card.setOwner(ownerUsername, state)
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stopAsync(): Deferred<Unit> = KtxAsync.async(asyncContext) {
		if (!isStopped)
		{
			startServerJob.join()
			isStopped = true
			server.stop()
			server.dispose()
		}
	}
	
	private inner class ServerListener : Listener
	{
		/** Contains ids of connections that have successfully performed a handshake. */
		private val handshookConnections = IntSet()
		private val runLater = GdxMap<String, IntMap<CancellableRunnable>>()
		
		override fun connected(connection: Connection)
		{
			connection.setName("Server-side client connection ${connection.id}")
			connection.setTimeout(0)
		}
		
		override fun disconnected(connection: Connection)
		{
			if (isStopped)
				return
			handshookConnections.remove(connection.id)
			if (connection.arbitraryData is User)
			{
				val user = connection.arbitraryData as User
				state.users.remove(user.name)
				actionLocks -= user.name
				if (state.hands[user.name]!!.isEmpty)
					state.hands.remove(user.name)
				runLater.remove(user.name)?.values()?.forEach { it.runnable() }
				for (serverObject: ServerObject in GdxArray(state.serverObjects))
				{
					if (serverObject is ServerLockable && serverObject.lockHolder == user.name)
						serverObject.lockHolder = null
					if (serverObject is ServerCard)
					{
						serverObject.justMoved = false
						serverObject.justRotated = false
						if (isGameStarted && serverObject.lastOwner == user.name)
						{
							serverObject.isFaceUp = true
							serverObject.lockHolder = null
							serverObject.setOwner(user.name, state)
							server.sendToAllTCP(ObjectOwnEvent(serverObject.id, user.name))
						}
					}
				}
				server.sendToAllTCP(UserLeftEvent(user))
				info("Server | INFO") { "${user.name} left the game" }
			}
		}
		
		@Suppress("UNCHECKED_CAST")
		override fun received(connection: Connection, `object`: Any)
		{
			if (isStopped)
				return
			if (connection.id !in handshookConnections) // Connections must perform handshake before packets are processed
			{
				if (`object` is Handshake)
				{
					val (versionString, data) = `object`
					if (versionString != VERSION_STRING) // Version check
					{
						connection.sendTCP(HandshakeReject("Incorrect version! Your Crazy Eights version is $versionString. Server version is $VERSION_STRING."))
						return
					}
					if (data?.size != 1) // Data integrity check
					{
						connection.sendTCP(HandshakeReject("Incorrect handshake data format! Expecting 1 argument but found ${data?.size}. This is a bug and shouldn't be happening, please notify the developer."))
						return
					}
					val username = data[0]
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
			
//			if (`object` !is FrameworkMessage.KeepAlive && `object` !is CursorPosition && `object` !is ObjectMoveEvent && `object` !is ObjectRotateEvent)
//				println("Server $`object`")
			val idToObjectMap = state.idToObjectMap
			when (`object`)
			{
				is User -> // A new user tries to join
				{
					connection.arbitraryData = `object`
					state.users[`object`.name] = `object`
					state.hands.getOrPut(`object`.name) { GdxArray() }
					connection.sendTCP(state)
					if (isGameStarted)
						connection.sendTCP(serverGameState!!.toGameState())
					server.sendToAllTCP(UserJoinedEvent(`object`))
					info("Server | INFO") { "${`object`.name} joined the game" }
				}
				is SwapSeatsEvent ->
				{
					val (user1, user2) = `object`
					val keys: GdxArray<String> = state.hands.orderedKeys()
					val index1 = keys.indexOf(user1, false)
					val index2 = keys.indexOf(user2, false)
					keys.swap(index1, index2)
					server.sendToAllTCP(`object`)
					info("Server | INFO") { "$user1 swapped seats with $user2" }
				}
				is AiAddEvent ->
				{
					aiCount++
					val name = aiNames.random() ?: "AI $aiCount"
					aiNames -= name
					val ai = User(name, Color.LIGHT_GRAY, true)
					state.users[name] = ai
					state.hands[name] = GdxArray()
					server.sendToAllTCP(UserJoinedEvent(ai))
				}
				is AiRemoveEvent ->
				{
					aiCount--
					val ai = state.users.remove(`object`.username)
					state.hands.remove(`object`.username)
					if (!`object`.username.startsWith("AI "))
						aiNames += `object`.username
					server.sendToAllTCP(UserLeftEvent(ai))
				}
				is Chat -> server.sendToAllTCP(`object`)
				is CursorPosition ->
				{
					server.sendToAllExceptTCP(connection.id, `object`)
					cursorPositionPool.free(`object`)
				}
				is TouchUpEvent -> server.sendToAllExceptTCP(connection.id, `object`)
				is ObjectLockEvent -> // User attempts to lock an object
				{
					if (actionLocks.isNotEmpty())
						return
					val (id, lockerUsername) = `object`
					val toLock = idToObjectMap[id]!!
					if (toLock !is ServerLockable || !toLock.canLock) // Only unlocked draggables can be locked
						return
					if (isGameStarted)
					{
						if (state.users[lockerUsername]!! !in serverGameState!!.playerHands)
							return
						if (toLock is ServerCard && toLock.cardGroupId != -1)
						{
							val cardGroup = idToObjectMap[toLock.cardGroupId] as ServerCardGroup
							if (cardGroup.cardHolderId == state.drawStackHolderId)
							{
								if (serverGameState!!.drawCount >= 3)
									return
								serverGameState!!.doMove(DrawMove)
							}
							else if (cardGroup.cardHolderId == state.discardPileHolderId)
								return
						}
						else if (toLock is ServerCardGroup)
							return
					}
					debug("Server | DEBUG") { "$lockerUsername locks $toLock" }
					toLock.lockHolder = lockerUsername
					toLock.toFront(state)
					server.sendToAllTCP(`object`)
				}
				is ObjectUnlockEvent -> // User unlocks an object
				{
					val (id, unlockerUsername, sideEffects) = `object`
					val toUnlock = idToObjectMap[id] ?: return
					if (toUnlock !is ServerLockable || toUnlock.lockHolder != unlockerUsername)
						return
					debug("Server | DEBUG") { "${toUnlock.lockHolder} unlocks $toUnlock" }
					toUnlock.lockHolder = null
					if (toUnlock is ServerCard)
					{
						if (toUnlock.cardGroupId != -1)
						{
							val cardGroup = idToObjectMap[toUnlock.cardGroupId] as ServerCardGroup
							if (isGameStarted && cardGroup.cardHolderId == state.drawStackHolderId)
							{
								cardGroup.draw(unlockerUsername)
								server.sendToAllTCP(`object`)
								server.sendToAllTCP(ObjectOwnEvent(id, unlockerUsername))
								return
							}
							else
								cardGroup.arrange()
						}
						else if (isGameStarted) // Restrict
						{
							toUnlock.lockHolder = ""
							runLater.getOrPut(unlockerUsername) { IntMap() }.put(
								toUnlock.id,
								CancellableRunnable(
									runnable = {
										toUnlock.isFaceUp = true
										toUnlock.lockHolder = null
										toUnlock.setOwner(unlockerUsername, state)
										server.sendToAllTCP(ObjectOwnEvent(id, unlockerUsername))
									},
									onCancel = { toUnlock.lockHolder = null }
								)
							)
						}
						if (!toUnlock.justMoved && !toUnlock.justRotated && sideEffects && !isGameStarted)
						{
							toUnlock.isFaceUp = !toUnlock.isFaceUp
							server.sendToAllTCP(CardFlipEvent(id))
						}
						toUnlock.justMoved = false
						toUnlock.justRotated = false
					}
					else if (toUnlock is ServerCardGroup)
					{
						if (toUnlock.cardHolderId != -1)
							toUnlock.rotation = 0F
					}
					server.sendToAllTCP(`object`)
				}
				is ObjectOwnEvent ->
				{
					val (id, ownerUsername) = `object`
					idToObjectMap[id]!!.setOwner(ownerUsername, state)
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
						toDisown.lockHolder = disownerUsername
					if (toDisown is ServerCard)
						toDisown.isFaceUp = isFaceUp
					state.serverObjects += toDisown
					state.hands[disownerUsername]!!.removeValue(toDisown, true)
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
					if (actionLocks.isNotEmpty())
						return
					val (id, x, y) = `object`
					val toMove = idToObjectMap[id]!!
					if (toMove !is ServerLockable || toMove.lockHolder?.let { state.users[it] } != connection.arbitraryData)
						return
					toMove.x = x
					toMove.y = y
					if (toMove is ServerCard)
						toMove.justMoved = true
					else if (toMove is ServerCardGroup)
					{
						if (toMove.isLocked && toMove.type == ServerCardGroup.Type.PILE)
							toMove.type = ServerCardGroup.Type.STACK
					}
					server.sendToAllExceptTCP(connection.id, `object`)
					objectMoveEventPool.free(`object`)
				}
				is ObjectRotateEvent ->
				{
					if (actionLocks.isNotEmpty())
						return
					val (id, rotation) = `object`
					val toRotate = idToObjectMap[id]!!
					if (toRotate !is ServerLockable || toRotate.lockHolder?.let { state.users[it] } != connection.arbitraryData)
						return
					if (toRotate is ServerCard)
						toRotate.justRotated = true
					toRotate.rotation = rotation
					server.sendToAllExceptTCP(connection.id, `object`)
					objectRotateEventPool.free(`object`)
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
						cardGroup.plusAssign(card, state)
					}
					cardGroup.arrange()
					state.addServerObject(cardGroup, insertAtIndex)
					server.sendToAllTCP(`object`.copy(id = cardGroup.id))
				}
				is CardGroupChangeEvent ->
				{
					val (cards, newCardGroupId, changerUsername) = `object`
					val newCardGroup = if (newCardGroupId != -1) idToObjectMap[newCardGroupId] as ServerCardGroup else null
					cards.forEachIndexed { index, (id, _, _, rotation) ->
						val card = idToObjectMap[id] as ServerCard
						if (isGameStarted && newCardGroup?.cardHolderId == state.discardPileHolderId)
						{
							val move = PlayMove(card)
							if (move !in serverGameState!!.moves)
								return
						}
						card.rotation = rotation
						state.hands[changerUsername]!!.removeValue(card, true)
						card.setServerCardGroup(newCardGroup, state)
						cards[index] = card
						if (isGameStarted)
							runLater.getOrPut(changerUsername) { IntMap() }.remove(id)?.onCancel?.invoke()
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
					cardGroup.cardHolderId = -1
					state.serverObjects += cardGroup
					val replacementCardGroup = ServerCardGroup(newId(), type = cardHolder.defaultType)
					idToObjectMap[replacementCardGroup.id] = replacementCardGroup
					cardHolder.cardGroup = replacementCardGroup
					replacementCardGroup.cardHolderId = cardHolder.id
					server.sendToAllTCP(CardGroupDetachEvent(cardHolderId, cardHolder.cardGroup.id, changerUsername))
				}
				is CardGroupDismantleEvent ->
				{
					val cardGroup = idToObjectMap[`object`.id] as ServerCardGroup
					while (cardGroup.cards.isNotEmpty())
					{
						val card: ServerCard = cardGroup.cards.removeIndex(0)
						card.setServerCardGroup(null, state)
					}
					idToObjectMap.remove(cardGroup.id)
					state.serverObjects.removeValue(cardGroup, true)
					server.sendToAllExceptTCP(connection.id, `object`)
				}
				is NewGameEvent ->
				{
					val drawStack = (idToObjectMap[state.drawStackHolderId] as ServerCardHolder).cardGroup
					val discardPile = (idToObjectMap[state.discardPileHolderId] as ServerCardHolder).cardGroup
					
					// Recall all cards
					val serverObjects = idToObjectMap.values().toArray()
					for (serverObject: ServerObject in serverObjects) // Unlock everything and move all cards to the draw stack
					{
						if (serverObject is ServerLockable)
							serverObject.lockHolder = null
						if (serverObject is ServerCard && serverObject.cardGroupId != drawStack.id)
						{
							serverObject.setServerCardGroup(drawStack, state)
							serverObject.isFaceUp = false
						}
					}
					for (serverObject: ServerObject in serverObjects) // Remove all empty card groups
					{
						if (serverObject is ServerCardGroup && serverObject.cardHolderId == -1)
						{
							idToObjectMap.remove(serverObject.id)
							state.serverObjects.removeValue(serverObject, true)
						}
					}
					for (username in state.hands.orderedKeys().toArray(String::class.java)) // Remove hands of offline users
					{
						if (username !in state.users)
							state.hands.remove(username)
					}
					state.hands.values().forEach { it.clear() }
					
					val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
					
					// Shuffle draw stack
					val seed = MathUtils.random.nextLong()
					drawStack.shuffle(seed, state)
					
					// Deal
					repeat(if (state.hands.size > 2) 5 else 7) {
						for (username: String in state.hands.orderedKeys())
							drawStack.draw(username)
					}
					val topCard: ServerCard = drawStack.cards.peek()
					topCard.setServerCardGroup(discardPile, state)
					topCard.isFaceUp = true
					
					// Set game state and action lock
					val playerHands = OrderedMap<Player, GdxArray<ServerCard>>()
					for ((username, hand) in state.hands)
					{
						val user = state.users[username]!!
						if (user.isAi)
							playerHands[IsmctsAgent(username)] = GdxArray(hand) as GdxArray<ServerCard>
						else
						{
							playerHands[user] = GdxArray(hand) as GdxArray<ServerCard>
							actionLocks += user.name
						}
					}
					serverGameState = ServerGameState(
						playerHands = playerHands,
						drawStack = GdxArray(drawStack.cards),
						discardPile = GdxArray(discardPile.cards)
					)
					
					server.sendToAllTCP(Chat(message = "Game started", isSystemMessage = true))
					server.sendToAllTCP(NewGameEvent(cardGroupChangeEvent, seed, serverGameState!!.toGameState()))
				}
				is NewGameActionFinishedEvent -> actionLocks -= (connection.arbitraryData as User).name
				is CrazyEightsClient.BufferEnd -> runLater.remove((connection.arbitraryData as User).name)?.values()?.forEach {
					it.runnable()
				}
			}
			
			state.updateDebugStrings()
		}
	}
	
	private class CancellableRunnable(val runnable: () -> Unit, val onCancel: () -> Unit)
}
