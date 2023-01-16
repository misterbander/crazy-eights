package misterbander.crazyeights.kryo

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryo.Kryo
import ktx.collections.*
import misterbander.crazyeights.net.client.CrazyEightsClient
import misterbander.crazyeights.net.packets.ActionLockReleaseEvent
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.CardGroupShuffleEvent
import misterbander.crazyeights.net.packets.CardSlideSoundEvent
import misterbander.crazyeights.net.packets.Chat
import misterbander.crazyeights.net.packets.CursorPosition
import misterbander.crazyeights.net.packets.DrawStackRefillEvent
import misterbander.crazyeights.net.packets.DrawTwoPenaltyEvent
import misterbander.crazyeights.net.packets.DrawTwosPlayedEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.GameEndedEvent
import misterbander.crazyeights.net.packets.GameState
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.PassEvent
import misterbander.crazyeights.net.packets.ResetDeckEvent
import misterbander.crazyeights.net.packets.ReversePlayedEvent
import misterbander.crazyeights.net.packets.RulesetUpdateEvent
import misterbander.crazyeights.net.packets.SkipsPlayedEvent
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.net.server.ServerCard
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.crazyeights.net.server.ServerCardGroup
import misterbander.crazyeights.net.server.ServerCardHolder
import misterbander.crazyeights.net.server.TabletopState
import misterbander.crazyeights.net.server.User
import misterbander.crazyeights.net.server.game.Ruleset

fun Kryo.registerClasses()
{
	register(Any::class.java)
	register(Array<String>::class.java)
	register(IntArray::class.java)
	register(FloatArray::class.java)
	register(GdxArray::class.java, GdxArraySerializer())
	register(GdxMap::class.java, GdxMapSerializer())
	register(OrderedMap::class.java, OrderedMapSerializer())
	register(Color::class.java)
	register(Handshake::class.java)
	register(HandshakeReject::class.java)
	register(User::class.java)
	register(UserJoinedEvent::class.java)
	register(UserLeftEvent::class.java)
	register(SwapSeatsEvent::class.java)
	register(AiAddEvent::class.java)
	register(AiRemoveEvent::class.java)
	register(TabletopState::class.java)
	register(Chat::class.java)
	register(CursorPosition::class.java).setInstantiator { cursorPositionPool.obtain() }
	register(TouchUpEvent::class.java)
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
	register(CardGroupShuffleEvent::class.java)
	register(NewGameEvent::class.java)
	register(GameEndedEvent::class.java)
	register(ActionLockReleaseEvent::class.java)
	register(Ruleset::class.java)
	register(RulesetUpdateEvent::class.java)
	register(GameState::class.java)
	register(CardSlideSoundEvent::class.java)
	register(PassEvent::class.java)
	register(EightsPlayedEvent::class.java)
	register(SuitDeclareEvent::class.java)
	register(DrawTwosPlayedEvent::class.java)
	register(DrawTwoPenaltyEvent::class.java)
	register(SkipsPlayedEvent::class.java)
	register(ReversePlayedEvent::class.java)
	register(DrawStackRefillEvent::class.java)
	register(ResetDeckEvent::class.java)
	register(CrazyEightsClient.BufferEnd::class.java)
}
