package misterbander.crazyeights.net.packets

import misterbander.crazyeights.kryo.KryoPoolable
import misterbander.crazyeights.kryo.objectRotateEventPool

data class ObjectRotateEvent(
	var id: Int = -1,
	var rotation: Float = 0F,
) : KryoPoolable
{
	override fun free() = objectRotateEventPool.free(this)
	
	override fun reset()
	{
		id = -1
		rotation = 0F
	}
}
