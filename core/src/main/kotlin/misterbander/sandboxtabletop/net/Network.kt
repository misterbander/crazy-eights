package misterbander.sandboxtabletop.net

import com.badlogic.gdx.graphics.Color
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.GdxSet
import ktx.scene2d.*
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.RoomState
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent

object Network
{
	var server: Server? = null
		set(value)
		{
			if (field != null && value != null)
				throw IllegalStateException("Overwriting nonnull server field")
			field = value?.apply {
				kryo.registerClasses()
				addListener(SandboxTabletopServerListener(value))
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
	
	private val asyncContext = newSingleThreadAsyncContext("Network-AsyncExecutor-Thread")
	var stopJob: Deferred<Unit>? = null
		private set
	
	private fun Kryo.registerClasses()
	{
		register(Array<String>::class.java)
		register(GdxSet::class.java, ObjectSetSerializer<GdxSet<Any>>())
		register(Handshake::class.java)
		register(HandshakeReject::class.java)
		register(User::class.java)
		register(Color::class.java)
		register(RoomState::class.java)
		register(UserJoinEvent::class.java)
		register(UserLeaveEvent::class.java)
		register(Chat::class.java)
		register(CursorPosition::class.java).setInstantiator { cursorPositionPool.obtain() }
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stop()
	{
		stopJob = KtxAsync.async(asyncContext) {
			server?.apply { stop(); dispose() }
			client?.apply { stop(); dispose() }
			server = null
			client = null
			stopJob = null
		}
	}
}
