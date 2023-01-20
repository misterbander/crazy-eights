package misterbander.crazyeights.net.server

import com.badlogic.gdx.utils.IntSet
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.esotericsoftware.kryonet.ServerDiscoveryHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.DEFAULT_UDP_PORT
import misterbander.crazyeights.VERSION_STRING
import misterbander.crazyeights.kryo.registerClasses
import misterbander.crazyeights.net.client.CrazyEightsClient
import misterbander.crazyeights.net.packets.ActionLockReleaseEvent
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.Chat
import misterbander.crazyeights.net.packets.CursorPosition
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
import misterbander.crazyeights.net.packets.ResetDeckEvent
import misterbander.crazyeights.net.packets.RulesetUpdateEvent
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.security.MessageDigest

class CrazyEightsServer(private val roomCode: String)
{
	val asyncContext = newSingleThreadAsyncContext("CrazyEightsServer-AsyncExecutor-Thread")
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
	@Volatile private var isStopped = false
	
	val tabletop: ServerTabletop
	private var maxId = 0
	
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
	
	suspend fun stop()
	{
		withContext(asyncContext) {
			if (isStopped)
				return@withContext
			info("Server | INFO") { "Stopping server..." }
			isStopped = true
			tabletop.aiJobs.forEach { it.cancel() }
			tabletop.aiJobs.clear()
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
				tabletop.removeUser(connection.arbitraryData as User)
		}
		
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
					if (tabletop.userCount >= 6) // Capacity check
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
				is User -> tabletop.onUserJoined(connection, `object`) // A new user tries to join
				is SwapSeatsEvent -> tabletop.onSwapSeats(`object`)
				is AiAddEvent -> tabletop.onAiAdd()
				is AiRemoveEvent -> tabletop.onAiRemove(`object`)
				is Chat -> server.sendToAllTCP(`object`)
				is CursorPosition ->
				{
					server.sendToAllExceptTCP(connection.id, `object`)
					`object`.free()
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
				is NewGameEvent -> tabletop.onNewGame(connection)
				is ActionLockReleaseEvent -> tabletop.onActionLockReleaseEvent(connection)
				is RulesetUpdateEvent -> tabletop.onRulesetUpdate(`object`)
				is PassEvent -> tabletop.pass()
				is SuitDeclareEvent -> tabletop.onSuitDeclare(connection, `object`)
				is ResetDeckEvent -> tabletop.onResetDeck()
				is CrazyEightsClient.BufferEnd -> tabletop.runLater.remove((connection.arbitraryData as User).name)?.values()
					?.forEach {
						it.runnable()
					}
			}
			
			tabletop.updateDebugStrings()
		}
	}
	
	class CancellableRunnable(val runnable: () -> Unit, val onCancel: () -> Unit)
}
