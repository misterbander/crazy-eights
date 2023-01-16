package misterbander.crazyeights.net

import com.badlogic.gdx.utils.Queue
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener

abstract class QueuedListener : Listener
{
	private val packetBuffer = Queue<Any>()
	
	override fun received(connection: Connection, `object`: Any) = synchronized(packetBuffer) {
		packetBuffer.addFirst(`object`)
	}
	
	fun handlePackets() = synchronized(packetBuffer) {
		while (packetBuffer.notEmpty())
			handlePacket(packetBuffer.removeLast())
	}
	
	abstract fun handlePacket(packet: Any)
}
