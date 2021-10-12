package misterbander.sandboxtabletop.net

import com.badlogic.gdx.utils.OrderedSet
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Listener
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.minusAssign
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectRotatedEvent

class SandboxTabletopClient
{
	val outgoingPacketBuffer = OrderedSet<Any>()
	
	private val asyncContext = newSingleThreadAsyncContext("SandboxTabletopClient-AsyncExecutor-Thread")
	private val client = Client().apply {
		kryo.registerClasses()
		setName("Client")
	}
	@Volatile private var isStopped = false
	
	fun connect(hostAddress: String, port: Int)
	{
		client.start()
		client.connect(hostAddress, port)
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stopAsync(): Deferred<Unit> = KtxAsync.async(asyncContext) {
		if (!isStopped)
		{
			client.stop()
			client.dispose()
			isStopped = true
		}
	}
	
	fun sendTCP(`object`: Any)
	{
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
		outgoingPacketBuffer.forEach { packet: Any ->
			sendTCP(packet)
			if (packet is ObjectMovedEvent)
				objectMovedEventPool.free(packet)
			else if (packet is ObjectRotatedEvent)
				objectRotatedEventPool.free(packet)
		}
		outgoingPacketBuffer.clear()
	}
	
	fun addListener(listener: Listener) = client.addListener(listener)
	
	fun removeListener(listener: Listener) = client.removeListener(listener)
}
