package misterbander.crazyeights.net.packets

import com.esotericsoftware.kryonet.Connection
import ktx.collections.*
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.net.KryoPoolable
import misterbander.crazyeights.net.objectRotateEventPool
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

fun Room.onObjectRotate(event: ObjectRotateEvent)
{
	val (id, rotation) = event
	tabletop.idToGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { rotationInterpolator.target = rotation }
	objectRotateEventPool.free(event)
}

fun CrazyEightsServer.onObjectRotate(connection: Connection, event: ObjectRotateEvent)
{
	if (actionLocks.isNotEmpty())
		return
	val (id, rotation) = event
	val toRotate = tabletop.idToObjectMap[id]!!
	if (toRotate !is ServerLockable || toRotate.lockHolder?.let { tabletop.users[it] } != connection.arbitraryData)
		return
	if (toRotate is ServerCard)
		toRotate.justRotated = true
	toRotate.rotation = rotation
	server.sendToAllExceptTCP(connection.id, event)
	objectRotateEventPool.free(event)
}
