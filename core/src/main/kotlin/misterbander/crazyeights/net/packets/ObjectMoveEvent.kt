package misterbander.crazyeights.net.packets

import com.esotericsoftware.kryonet.Connection
import ktx.collections.*
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.net.KryoPoolable
import misterbander.crazyeights.net.ServerTabletop
import misterbander.crazyeights.net.objectMoveEventPool
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.modules.SmoothMovable

data class ObjectMoveEvent(
	var id: Int = -1,
	var x: Float = 0F,
	var y: Float = 0F
) : KryoPoolable
{
	override fun reset()
	{
		id = -1
		x = 0F
		y = 0F
	}
}

fun Tabletop.onObjectMove(event: ObjectMoveEvent)
{
	val (id, x, y) = event
	val toMove = idToGObjectMap[id]!!
	toMove.getModule<SmoothMovable>()?.setPosition(x, y)
	if (toMove is CardGroup && toMove.type == ServerCardGroup.Type.PILE)
	{
		toMove.type = ServerCardGroup.Type.STACK
		toMove.arrange()
	}
	objectMoveEventPool.free(event)
}

fun ServerTabletop.onObjectMove(connection: Connection, event: ObjectMoveEvent)
{
	if (parent.actionLocks.isNotEmpty())
		return
	val (id, x, y) = event
	val toMove = idToObjectMap[id]!!
	if (toMove !is ServerLockable || toMove.lockHolder?.let { users[it] } != connection.arbitraryData)
		return
	toMove.x = x
	toMove.y = y
	if (toMove is ServerCard)
		toMove.justMoved = true
	else if (toMove is ServerCardGroup)
	{
		if (toMove.isLocked && toMove.type == ServerCardGroup.Type.PILE)
			toMove.type = ServerCardGroup.Type.STACK
	}
	parent.server.sendToAllExceptTCP(connection.id, event)
	objectMoveEventPool.free(event)
}
