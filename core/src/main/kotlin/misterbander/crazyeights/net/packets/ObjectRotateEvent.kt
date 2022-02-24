package misterbander.crazyeights.net.packets

import com.esotericsoftware.kryonet.Connection
import ktx.collections.*
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.kryo.KryoPoolable
import misterbander.crazyeights.net.ServerTabletop
import misterbander.crazyeights.kryo.objectRotateEventPool
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.modules.SmoothMovable

data class ObjectRotateEvent(
	var id: Int = -1,
	var rotation: Float = 0F,
) : KryoPoolable
{
	override fun reset()
	{
		id = -1
		rotation = 0F
	}
}

fun Tabletop.onObjectRotate(event: ObjectRotateEvent)
{
	val (id, rotation) = event
	idToGObjectMap[id]!!.getModule<SmoothMovable>()?.rotation = rotation
	objectRotateEventPool.free(event)
}

fun ServerTabletop.onObjectRotate(connection: Connection, event: ObjectRotateEvent)
{
	if (parent.actionLocks.isNotEmpty())
		return
	val (id, rotation) = event
	val toRotate = idToObjectMap[id]!!
	if (toRotate !is ServerLockable || toRotate.lockHolder?.let { users[it] } != connection.arbitraryData)
		return
	if (toRotate is ServerCard)
		toRotate.justRotated = true
	toRotate.rotation = rotation
	parent.server.sendToAllExceptTCP(connection.id, event)
	objectRotateEventPool.free(event)
}
