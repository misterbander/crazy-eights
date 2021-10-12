package misterbander.sandboxtabletop.net

import com.badlogic.gdx.utils.Queue
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener

abstract class BufferedListener : Listener
{
	private val packetBuffer = Queue<Any>()
	
	override fun received(connection: Connection, `object`: Any) = packetBuffer.addLast(`object`)
	
	fun processPackets()
	{
		while (packetBuffer.notEmpty())
			processPacket(packetBuffer.removeFirst())
	}
	
	abstract fun processPacket(packet: Any)
}
