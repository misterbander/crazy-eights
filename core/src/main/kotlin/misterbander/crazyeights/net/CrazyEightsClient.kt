package misterbander.crazyeights.net

import com.badlogic.gdx.utils.OrderedSet
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.ClientDiscoveryHandler
import com.esotericsoftware.kryonet.Listener
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.*
import misterbander.crazyeights.DEFAULT_TCP_PORT
import misterbander.crazyeights.DEFAULT_UDP_PORT
import misterbander.crazyeights.kryo.objectMoveEventPool
import misterbander.crazyeights.kryo.objectRotateEventPool
import misterbander.crazyeights.kryo.registerClasses
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
	
	fun connect(hostAddress: String, port: Int)
	{
		client.start()
		// Temporary workaround to avoid crashing the application by catching the annoying
		// java.nio.channels.ClosedSelectorException caused when closing the server
		client.updateThread.setUncaughtExceptionHandler { _, e: Throwable -> e.printStackTrace() }
		client.connect(500, hostAddress, port, DEFAULT_UDP_PORT)
	}
	
	fun discoverHostByRoomCode(roomCode: String)
	{
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
					connect(datagramPacket.address.hostAddress, DEFAULT_TCP_PORT)
				}
			}
		})
		repeat(4) {
			client.discoverHosts(DEFAULT_UDP_PORT, 5000)
			if (found || isStopped)
				return
		}
		throw ConnectException("Couldn't find server with room code: $roomCode")
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stop(): Job = KtxAsync.launch(asyncContext) {
		if (!isStopped)
		{
			isStopped = true
			client.stop()
			client.dispose()
		}
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
	
	fun addListener(listener: Listener) = client.addListener(listener)
	
	fun removeListener(listener: Listener) = client.removeListener(listener)
	
	object BufferEnd
}
