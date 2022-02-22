package misterbander.crazyeights.net.packets

import com.esotericsoftware.kryonet.Connection
import ktx.collections.*
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.ServerOwnable
import misterbander.crazyeights.net.ServerTabletop
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.Groupable
import misterbander.crazyeights.scene2d.MyHand
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.crazyeights.scene2d.modules.SmoothMovable

@NoArg
data class ObjectOwnEvent(val id: Int, val ownerUsername: String)

@NoArg
data class ObjectDisownEvent(
	val id: Int = -1,
	val x: Float = 0F,
	val y: Float = 0F,
	val rotation: Float = 0F,
	val isFaceUp: Boolean = false,
	val disownerUsername: String
)

@Suppress("UNCHECKED_CAST")
fun Tabletop.onObjectOwn(event: ObjectOwnEvent)
{
	val (id, ownerUsername) = event
	val toOwn = idToGObjectMap[id] as Groupable<CardGroup>
	val hand = userToHandMap[ownerUsername]!!
	toOwn.getModule<Lockable>()?.unlock()
	(toOwn.parent as? CardGroup)?.minusAssign(toOwn)
	hand += toOwn
	if (hand is MyHand)
	{
		toOwn.getModule<Ownable>()?.wasInHand = true
		if (toOwn is Card)
			toOwn.isFaceUp = true
		hand.arrange(false)
	}
	else
		hand.arrange()
}

fun ServerTabletop.onObjectOwn(connection: Connection, event: ObjectOwnEvent)
{
	val (id, ownerUsername) = event
	(idToObjectMap[id] as ServerOwnable).setOwner(this, ownerUsername)
	parent.server.sendToAllExceptTCP(connection.id, event)
}

@Suppress("UNCHECKED_CAST")
fun Tabletop.onObjectDisown(event: ObjectDisownEvent)
{
	val (id, x, y, rotation, isFaceUp, disownerUsername) = event
	val toDisown = idToGObjectMap[id] as Groupable<CardGroup>
	val hand = userToHandMap[disownerUsername]!!
	hand -= toDisown
	hand.arrange()
	toDisown.getModule<SmoothMovable>()?.apply {
		setPosition(x, y)
		this.rotation = rotation
	}
	toDisown.getModule<Lockable>()?.lock(users[disownerUsername]!!)
	if (toDisown is Card)
		toDisown.isFaceUp = isFaceUp
}

fun ServerTabletop.onObjectDisown(connection: Connection, event: ObjectDisownEvent)
{
	val (id, x, y, rotation, isFaceUp, disownerUsername) = event
	val toDisown = idToObjectMap[id]!!
	toDisown.x = x
	toDisown.y = y
	toDisown.rotation = rotation
	if (toDisown is ServerLockable)
		toDisown.lockHolder = disownerUsername
	if (toDisown is ServerCard)
		toDisown.isFaceUp = isFaceUp
	serverObjects += toDisown
	hands[disownerUsername]!!.removeValue(toDisown, true)
	parent.server.sendToAllExceptTCP(connection.id, event)
}

@NoArg
data class HandUpdateEvent(val hand: GdxArray<ServerObject>, val ownerUsername: String)

fun ServerTabletop.onHandUpdate(connection: Connection, event: HandUpdateEvent)
{
	val (hand, ownerUsername) = event
	hands[ownerUsername] = hand
	hand.forEach { idToObjectMap[it.id] = it }
	parent.server.sendToAllExceptTCP(connection.id, event)
}
