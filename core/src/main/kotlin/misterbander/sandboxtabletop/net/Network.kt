package misterbander.sandboxtabletop.net

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import ktx.collections.GdxSet
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.RoomState
import org.objenesis.instantiator.ObjectInstantiator
import java.util.UUID

object Network
{
	var server: Server? = null
		set(value)
		{
			if (field != null && value != null)
				throw IllegalStateException("Overwriting nonnull server field")
			field = value?.apply {
				kryo.registerClasses()
				addListener(SandboxTabletopServerListener())
			}
		}
	var client: Client? = null
		set(value)
		{
			if (field != null && value != null)
				throw IllegalStateException("Overwriting nonnull client field")
			field = value?.apply {
				kryo.registerClasses()
				setName("Client")
			}
		}
	
	private fun Kryo.registerClasses()
	{
		register(Array<String>::class.java)
		register(Handshake::class.java)
		register(HandshakeReject::class.java)
		register(UUID::class.java).instantiator = ObjectInstantiator { UUID(0, 0) }
		register(User::class.java)
		register(GdxSet::class.java, ObjectSetSerializer<GdxSet<Any>>())
		register(RoomState::class.java)
	}
	
	fun stop()
	{
		server?.apply { close(); dispose() }
		server = null
		client?.apply { close(); dispose() }
		client = null
	}
}
