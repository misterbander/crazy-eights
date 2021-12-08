package misterbander.crazyeights.net

import com.badlogic.gdx.utils.Queue
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener

abstract class BufferedListener : Listener
{
	private val packetBuffer = Queue<Any>()
	
	override fun received(connection: Connection, `object`: Any) = synchronized(packetBuffer) {
		packetBuffer.addFirst(`object`)
	}
	
	fun processPackets() = synchronized(packetBuffer) {
		while (packetBuffer.notEmpty())
			processPacket(packetBuffer.removeLast())
	}
	
	abstract fun processPacket(packet: Any)
}
