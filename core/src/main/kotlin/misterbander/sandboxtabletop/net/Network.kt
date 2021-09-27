package misterbander.sandboxtabletop.net

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.GdxArray
import ktx.scene2d.*
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.FlipCardEvent
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent

class Network(val server: Server?, val client: Client)
{
	private val asyncContext = newSingleThreadAsyncContext("Network-AsyncExecutor-Thread")
	
	init
	{
		server?.apply {
			kryo.registerClasses()
			addListener(RoomServerListener(this))
		}
		client.kryo.registerClasses()
		client.setName("Client")
	}
	
	private fun Kryo.registerClasses()
	{
		register(Any::class.java)
		register(Array<String>::class.java)
		register(GdxArray::class.java, GdxArraySerializer())
		register(OrderedMap::class.java, OrderedMapSerializer())
		register(Handshake::class.java)
		register(HandshakeReject::class.java)
		register(User::class.java)
		register(Color::class.java)
		register(Chat::class.java)
		register(UserJoinEvent::class.java)
		register(UserLeaveEvent::class.java)
		register(TabletopState::class.java)
		register(CursorPosition::class.java).setInstantiator { cursorPositionPool.obtain() }
		register(ServerCard::class.java)
		register(ServerCard.Rank::class.java)
		register(ServerCard.Suit::class.java)
		register(ObjectLockEvent::class.java)
		register(ObjectUnlockEvent::class.java)
		register(ObjectMovedEvent::class.java).setInstantiator { objectMovedEventPool.obtain() }
		register(FlipCardEvent::class.java)
	}
	
	@Suppress("BlockingMethodInNonBlockingContext")
	fun stopAsync(): Deferred<Unit> = KtxAsync.async(asyncContext) {
		server?.apply { stop(); dispose() }
		client.apply { stop(); dispose() }
	}
}
