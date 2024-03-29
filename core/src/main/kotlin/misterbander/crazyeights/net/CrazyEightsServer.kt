package misterbander.crazyeights.net

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.Queue
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.esotericsoftware.kryonet.ServerDiscoveryHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.DEFAULT_UDP_PORT
import misterbander.crazyeights.VERSION_STRING
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.DrawMove
import misterbander.crazyeights.game.DrawTwoEffectPenalty
import misterbander.crazyeights.game.PassMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.ai.IsmctsAgent
import misterbander.crazyeights.game.draw
import misterbander.crazyeights.game.pass
import misterbander.crazyeights.game.play
import misterbander.crazyeights.kryo.cursorPositionPool
import misterbander.crazyeights.kryo.registerClasses
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.ActionLockReleaseEvent
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.PassEvent
import misterbander.crazyeights.net.packets.PowerCardPlayedEvent
import misterbander.crazyeights.net.packets.ResetDeckEvent
import misterbander.crazyeights.net.packets.RulesetUpdateEvent
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.net.packets.acceptDrawTwoPenalty
import misterbander.crazyeights.net.packets.onAiAdd
import misterbander.crazyeights.net.packets.onAiRemove
import misterbander.crazyeights.net.packets.onCardGroupChange
import misterbander.crazyeights.net.packets.onCardGroupCreate
import misterbander.crazyeights.net.packets.onCardGroupDetach
import misterbander.crazyeights.net.packets.onCardGroupDismantle
import misterbander.crazyeights.net.packets.onHandUpdate
import misterbander.crazyeights.net.packets.onNewGame
import misterbander.crazyeights.net.packets.onObjectDisown
import misterbander.crazyeights.net.packets.onObjectLock
import misterbander.crazyeights.net.packets.onObjectMove
import misterbander.crazyeights.net.packets.onObjectOwn
import misterbander.crazyeights.net.packets.onObjectRotate
import misterbander.crazyeights.net.packets.onObjectUnlock
import misterbander.crazyeights.net.packets.onResetDeck
import misterbander.crazyeights.net.packets.onRulesetUpdate
import misterbander.crazyeights.net.packets.onSuitDeclare
import misterbander.crazyeights.net.packets.onSwapSeats
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.MessageDigest

@Suppress("BlockingMethodInNonBlockingContext")
class CrazyEightsServer(private val roomCode: String)
{
	private val asyncContext = newSingleThreadAsyncContext("CrazyEightsServer-AsyncExecutor-Thread")
	val server by lazy {
		Server().apply {
			kryo.registerClasses()
			addListener(ServerListener())
			setDiscoveryHandler(object : ServerDiscoveryHandler
			{
				override fun onDiscoverHost(datagramChannel: DatagramChannel, fromAddress: InetSocketAddress): Boolean
				{
					// Client discovery. Send back hashed room code.
					val bytes = roomCode.toByteArray()
					val md = MessageDigest.getInstance("SHA-256")
					val digest: ByteArray = md.digest(bytes)
					datagramChannel.send(ByteBuffer.wrap(digest), fromAddress)
//					println(digest.contentToString())
					return true
				}
			})
		}
	}
	private val startServerJob = Job()
	val aiJobs = Queue<Job>()
	@Volatile private var isStopped = false
	
	val tabletop: ServerTabletop
	private var maxId = 0
	val actionLocks = GdxSet<String>()
	val runLater = GdxMap<String, IntMap<CancellableRunnable>>()
	val aiNames = gdxArrayOf("Shark (AI)", "Queenpin (AI)", "Watson (AI)", "Ning (AI)")
	var aiCount = 0
	var ruleset: Ruleset = Ruleset(firstDiscardOnDealTriggersPower = true)
	var serverGameState: ServerGameState? = null
	val isGameStarted: Boolean
		get() = serverGameState != null
	var lastPowerCardPlayedEvent: PowerCardPlayedEvent? = null
	
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
		val drawStackHolder =
			ServerCardHolder(newId(), x = 540F, y = 360F, cardGroup = ServerCardGroup(newId(), cards = deck))
		val discardPileHolder = ServerCardHolder(
			newId(),
			x = 740F,
			y = 360F,
			cardGroup = ServerCardGroup(newId(), type = ServerCardGroup.Type.PILE)
		)
		tabletop = ServerTabletop(this, drawStackHolder, discardPileHolder)
		tabletop.addServerObject(drawStackHolder)
		tabletop.addServerObject(discardPileHolder)
		
		debug("Server | DEBUG") { "Initialized Room server" }
	}
	
	fun newId(): Int = maxId++
	
	suspend fun start(port: Int) = withContext(asyncContext) {
		info("Server | INFO") { "Starting server on TCP port $port and UDP port $DEFAULT_UDP_PORT..." }
		server.bind(port, DEFAULT_UDP_PORT)
		server.start()
	}
	
	/**
	 * Some actions play a client-side animation which takes some time. While the animation is playing, we must ensure
	 * that no other events take place to prevent events from overlapping, causing strange behavior. This is achieved
	 * using action locks.
	 * If an event that plays a client-side animation occurs, each currently online user will obtain an action lock.
	 * Action locks will only be released once the client-side animation finishes, or the user leaves the room.
	 */
	fun acquireActionLocks()
	{
		for ((username, user) in tabletop.users)
		{
			if (!user!!.isAi)
				actionLocks += username
		}
		debug("Server | DEBUG") { "Acquired action locks: $actionLocks" }
	}
	
	suspend fun waitForActionLocks()
	{
		debug("Server | DEBUG") { "Waiting for action locks: remaining = $actionLocks" }
		while (true)
		{
			if (actionLocks.isEmpty)
				break
			delay(1000/60)
		}
	}
	
	fun onPlayerChanged(player: Player)
	{
		if (!isGameStarted)
			return
		val serverGameState = serverGameState!!
		val drawStack = tabletop.drawStackHolder.cardGroup
		val discardPile = tabletop.discardPileHolder.cardGroup
		if (tabletop.users[player.name]?.isAi != true)
			return
		
		val firstAiJob = if (!aiJobs.isEmpty) aiJobs.first() else null
		aiJobs.addFirst(KtxAsync.launch {
			firstAiJob?.join()
			var justDrew = false
			do
			{
				val moveDeferred = async(asyncContext) { (player as IsmctsAgent).getMove(serverGameState) }
				delay(if (justDrew) 800 else lastPowerCardPlayedEvent?.delayMillis ?: 1000)
				waitForActionLocks()
				justDrew = false
				lastPowerCardPlayedEvent = null
				val move = moveDeferred.await()
				info("Server | DEBUG") { "${player.name} best move = $move" }
				when (move)
				{
					is PlayMove ->
					{
						val card = tabletop.idToObjectMap[move.card.id] as ServerCard
						play(CardGroupChangeEvent(gdxArrayOf(card), discardPile.id, player.name))
					}
					is ChangeSuitMove ->
					{
						val card = tabletop.idToObjectMap[move.card.id] as ServerCard
						play(CardGroupChangeEvent(gdxArrayOf(card), discardPile.id, player.name))
						delay(3000)
						onSuitDeclare(event = SuitDeclareEvent(move.declaredSuit))
					}
					is DrawMove ->
					{
						val drawnCard: ServerCard = drawStack.cards.peek()
						serverGameState.doMove(move)
						draw(drawnCard, player.name, fireOwnEvent = true, playSound = true)
						justDrew = true
					}
					is DrawTwoEffectPenalty -> acceptDrawTwoPenalty(player.name)
					is PassMove -> pass()
				}
			}
			while (justDrew)
			if (!isActive)
				throw CancellationException()
			aiJobs.removeLast()
		})
	}
	
	suspend fun stop()
	{
		withContext(asyncContext) {
			if (isStopped)
				return@withContext
			info("Server | INFO") { "Stopping server..." }
			isStopped = true
			aiJobs.forEach { it.cancel() }
			aiJobs.clear()
			server.stop()
			server.dispose()
		}
		asyncContext.dispose()
		info("Server | INFO") { "Server stopped!" }
	}
	
	private inner class ServerListener : Listener
	{
		/** Contains ids of connections that have successfully performed a handshake. */
		private val handshookConnections = IntSet()
		
		override fun connected(connection: Connection)
		{
			connection.setName("Server-side client connection ${connection.id}")
//			connection.setTimeout(0)
		}
		
		override fun disconnected(connection: Connection)
		{
			if (isStopped)
				return
			handshookConnections.remove(connection.id)
			if (connection.arbitraryData is User)
			{
				val user = connection.arbitraryData as User
				tabletop.users.remove(user.name)
				actionLocks -= user.name
				if (tabletop.hands[user.name]!!.isEmpty)
					tabletop.hands.remove(user.name)
				runLater.remove(user.name)?.values()?.forEach { it.runnable() }
				for (serverObject: ServerObject in GdxArray(tabletop.serverObjects))
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
							serverObject.setOwner(tabletop, user.name)
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
						connection.close()
						return
					}
					if (data?.size != 2) // Data integrity check
					{
						connection.sendTCP(HandshakeReject("Incorrect handshake data format! Expecting 2 arguments but found ${data?.size}. This is a bug and shouldn't be happening, please notify the developer."))
						connection.close()
						return
					}
					val (username, roomCode) = data
					if (roomCode != this@CrazyEightsServer.roomCode) // Verify room code
					{
						connection.sendTCP(HandshakeReject("Incorrect room code."))
						connection.close()
						return
					}
					if (tabletop.users.size >= 6) // Capacity check
					{
						connection.sendTCP(HandshakeReject("Room is already full (Max 6 players)."))
						connection.close()
						return
					}
					if (tabletop.users[username] != null) // Check username collision
					{
						connection.sendTCP(HandshakeReject("Username conflict! Username $username is already taken."))
						connection.close()
						return
					}
					if (username.isBlank())
					{
						connection.sendTCP(HandshakeReject("Illegal username \"\"."))
						connection.close()
						return
					}
					
					// Handshake is successful
					handshookConnections.add(connection.id)
					connection.sendTCP(Handshake())
					info("Server | INFO") { "Successful handshake from $connection" }
				}
				else
				{
					connection.close()
					ktx.log.error("Server | ERROR") { "$connection attempted to send objects before handshake" }
				}
				return
			}

//			if (`object` !is FrameworkMessage.KeepAlive && `object` !is CursorPosition && `object` !is ObjectMoveEvent && `object` !is ObjectRotateEvent)
//				println("Server $`object`")
			when (`object`)
			{
				is User -> // A new user tries to join
				{
					connection.arbitraryData = `object`
					tabletop.users[`object`.name] = `object`
					tabletop.hands.getOrPut(`object`.name) { GdxArray() }
					connection.sendTCP(tabletop.toTabletopState())
					connection.sendTCP(RulesetUpdateEvent(ruleset))
					if (isGameStarted)
						connection.sendTCP(
							serverGameState!!.toGameState(
								if (tabletop.suitChooser != null) EightsPlayedEvent(tabletop.suitChooser!!) else null
							)
						)
					server.sendToAllTCP(UserJoinedEvent(`object`))
					info("Server | INFO") { "${`object`.name} joined the game" }
				}
				is SwapSeatsEvent -> tabletop.onSwapSeats(`object`)
				is AiAddEvent -> tabletop.onAiAdd()
				is AiRemoveEvent -> tabletop.onAiRemove(`object`)
				is Chat -> server.sendToAllTCP(`object`)
				is CursorPosition ->
				{
					server.sendToAllExceptTCP(connection.id, `object`)
					cursorPositionPool.free(`object`)
				}
				is TouchUpEvent -> server.sendToAllExceptTCP(connection.id, `object`)
				is ObjectLockEvent -> tabletop.onObjectLock(`object`) // User attempts to lock an object
				is ObjectUnlockEvent -> tabletop.onObjectUnlock(`object`) // User unlocks an object
				is ObjectOwnEvent -> tabletop.onObjectOwn(connection, `object`)
				is ObjectDisownEvent -> tabletop.onObjectDisown(connection, `object`)
				is HandUpdateEvent -> tabletop.onHandUpdate(connection, `object`)
				is ObjectMoveEvent -> tabletop.onObjectMove(connection, `object`)
				is ObjectRotateEvent -> tabletop.onObjectRotate(connection, `object`)
				is CardGroupCreateEvent -> tabletop.onCardGroupCreate(`object`)
				is CardGroupChangeEvent -> tabletop.onCardGroupChange(`object`)
				is CardGroupDetachEvent -> tabletop.onCardGroupDetach(`object`)
				is CardGroupDismantleEvent -> tabletop.onCardGroupDismantle(connection, `object`)
				is NewGameEvent -> onNewGame(connection)
				is ActionLockReleaseEvent -> actionLocks -= (connection.arbitraryData as User).name
				is RulesetUpdateEvent -> onRulesetUpdate(`object`)
				is PassEvent -> pass()
				is SuitDeclareEvent -> onSuitDeclare(connection, `object`)
				is ResetDeckEvent -> tabletop.onResetDeck()
				is CrazyEightsClient.BufferEnd -> runLater.remove((connection.arbitraryData as User).name)?.values()
					?.forEach {
						it.runnable()
					}
			}
			
			tabletop.updateDebugStrings()
		}
	}
	
	class CancellableRunnable(val runnable: () -> Unit, val onCancel: () -> Unit)
}
