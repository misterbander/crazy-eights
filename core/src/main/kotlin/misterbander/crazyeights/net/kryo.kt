package misterbander.crazyeights.net

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryo.Kryo
import ktx.collections.GdxArray
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardGroupChangedEvent
import misterbander.crazyeights.net.packets.CardGroupCreatedEvent
import misterbander.crazyeights.net.packets.CardGroupDismantledEvent
import misterbander.crazyeights.net.packets.FlipCardEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMovedEvent
import misterbander.crazyeights.net.packets.ObjectRotatedEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.UserJoinEvent
import misterbander.crazyeights.net.packets.UserLeaveEvent

fun Kryo.registerClasses()
{
	register(Any::class.java)
	register(Array<String>::class.java)
	register(IntArray::class.java)
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
	register(Rank::class.java)
	register(Suit::class.java)
	register(ServerCardGroup::class.java)
	register(ServerCardGroup.Type::class.java)
	register(ObjectLockEvent::class.java)
	register(ObjectUnlockEvent::class.java)
	register(ObjectMovedEvent::class.java).setInstantiator { objectMovedEventPool.obtain() }
	register(ObjectRotatedEvent::class.java).setInstantiator { objectRotatedEventPool.obtain() }
	register(FlipCardEvent::class.java)
	register(CardGroupCreatedEvent::class.java)
	register(CardGroupChangedEvent::class.java)
	register(CardGroupDismantledEvent::class.java)
}
