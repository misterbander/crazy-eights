package misterbander.crazyeights.net.client

import com.badlogic.gdx.utils.OrderedSet
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.ClientDiscoveryHandler
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.DEFAULT_TCP_PORT
import misterbander.crazyeights.DEFAULT_UDP_PORT
import misterbander.crazyeights.kryo.objectMoveEventPool
import misterbander.crazyeights.kryo.objectRotateEventPool
import misterbander.crazyeights.kryo.registerClasses
import misterbander.crazyeights.net.ListenerContainer
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import java.net.ConnectException
import java.net.DatagramPacket
import java.security.MessageDigest

class CrazyEightsClient
{
	val outgoingPacketBuffer = OrderedSet<Any>()
	
	private val asyncContext = newSingleThreadAsyncContext("CrazyEightsClient-AsyncExecutor-Thread")
	private val client by lazy {
		Client().apply {
			kryo.registerClasses()
			setName("Client")
		}
	}
	@Volatile private var isStopped = false
	
	suspend fun connect(
		hostAddress: String,
		port: Int,
		timeout: Int = 3000,
		retryInterval: Long = 3000,
		maxRetries: Int = Int.MAX_VALUE
	) = coroutineScope {
		info("Client | INFO") { "Connecting to $hostAddress on TCP port $port and UDP port $DEFAULT_UDP_PORT..." }
		var connected = false
		var retries = 0
		while (!connected && retries < maxRetries)
		{
			yield()
			if (retries > 0)
				info("Client | INFO") { "Retrying... (Retried $retries ${if (retries == 1) "time" else "times"})" }
			val connectJob = launch(asyncContext) {
				client.start()
				// Temporary workaround to avoid crashing the application by catching the annoying
				// java.nio.channels.ClosedSelectorException caused when closing the server
				client.updateThread.setUncaughtExceptionHandler { _, e: Throwable -> e.printStackTrace() }
				try
				{
					client.connect(timeout, hostAddress, port, DEFAULT_UDP_PORT)
					connected = true
				}
				catch (e: Exception)
				{
					if (retries == maxRetries - 1)
					{
						yield()
						throw e
					}
				}
			}
			val waitToRetryJob = launch {
				delay(retryInterval)
				info("Client | INFO") { "Client connection too slow! Aborting..." }
				client.stop()
				connectJob.cancel()
			}
			connectJob.join()
			waitToRetryJob.cancel()
			retries++
		}
	}
	
	suspend fun discoverHostByRoomCode(roomCode: String) = coroutineScope {
		var found = false
		client.setDiscoveryHandler(object : ClientDiscoveryHandler
		{
			override fun onRequestNewDatagramPacket(): DatagramPacket = DatagramPacket(ByteArray(32), 32)
			
			override fun onDiscoveredHost(datagramPacket: DatagramPacket)
			{
				if (found || isStopped)
					return
				val bytes = roomCode.toByteArray()
				val md = MessageDigest.getInstance("SHA-256")
				val digest: ByteArray = md.digest(bytes)
				if (digest.contentEquals(datagramPacket.data))
				{
					found = true
					launch { connect(datagramPacket.address.hostAddress, DEFAULT_TCP_PORT, maxRetries = 5) }
				}
			}
		})
		repeat(4) {
			yield()
			withContext(asyncContext) { client.discoverHosts(DEFAULT_UDP_PORT, 5000) }
			if (found || isStopped)
				return@coroutineScope
		}
		throw ConnectException("Couldn't find server with room code: $roomCode")
	}
	
	suspend fun stop()
	{
		withContext(asyncContext) {
			if (isStopped)
				return@withContext
			info("Client | INFO") { "Stopping client..." }
			isStopped = true
			client.stop()
			client.dispose()
		}
		asyncContext.dispose()
		info("Client | INFO") { "Client stopped!" }
	}
	
	fun sendTCP(`object`: Any)
	{
		if (!isStopped)
			client.sendTCP(`object`)
	}
	
	inline fun <reified T> removeFromOutgoingPacketBuffer(crossinline predicate: (T) -> Boolean): T?
	{
		val packet = outgoingPacketBuffer.find { it is T && predicate(it) }
		if (packet != null)
			outgoingPacketBuffer -= packet
		return packet as T?
	}
	
	fun flushOutgoingPacketBuffer()
	{
		val isEmpty = outgoingPacketBuffer.isEmpty
		for (packet: Any in outgoingPacketBuffer)
		{
			sendTCP(packet)
			if (packet is ObjectMoveEvent)
				objectMoveEventPool.free(packet)
			else if (packet is ObjectRotateEvent)
				objectRotateEventPool.free(packet)
		}
		if (!isEmpty)
			sendTCP(BufferEnd)
		outgoingPacketBuffer.clear()
	}
	
	fun addListener(listenerContainer: ListenerContainer<*>) = client.addListener(listenerContainer.registerNewListener())
	
	fun removeListener(listenerContainer: ListenerContainer<*>) = client.removeListener(listenerContainer.unregisterListener())
	
	object BufferEnd
}
