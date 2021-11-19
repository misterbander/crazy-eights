package misterbander.crazyeights.net

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryo.Kryo
import ktx.collections.*
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent

fun Kryo.registerClasses()
{
	register(Any::class.java)
	register(Array<String>::class.java)
	register(IntArray::class.java)
	register(FloatArray::class.java)
	register(GdxArray::class.java, GdxArraySerializer())
	register(GdxMap::class.java, GdxMapSerializer())
	register(OrderedMap::class.java, OrderedMapSerializer())
	register(Handshake::class.java)
	register(HandshakeReject::class.java)
	register(User::class.java)
	register(Color::class.java)
	register(Chat::class.java)
	register(UserJoinedEvent::class.java)
	register(UserLeftEvent::class.java)
	register(TabletopState::class.java)
	register(CursorPosition::class.java).setInstantiator { cursorPositionPool.obtain() }
	register(ServerCard::class.java)
	register(Rank::class.java)
	register(Suit::class.java)
	register(ServerCardGroup::class.java)
	register(ServerCardGroup.Type::class.java)
	register(ServerCardHolder::class.java)
	register(ObjectLockEvent::class.java)
	register(ObjectUnlockEvent::class.java)
	register(ObjectOwnEvent::class.java)
	register(ObjectDisownEvent::class.java)
	register(HandUpdateEvent::class.java)
	register(ObjectMoveEvent::class.java).setInstantiator { objectMoveEventPool.obtain() }
	register(ObjectRotateEvent::class.java).setInstantiator { objectRotateEventPool.obtain() }
	register(CardFlipEvent::class.java)
	register(CardGroupCreateEvent::class.java)
	register(CardGroupChangeEvent::class.java)
	register(CardGroupDetachEvent::class.java)
	register(CardGroupDismantleEvent::class.java)
}
