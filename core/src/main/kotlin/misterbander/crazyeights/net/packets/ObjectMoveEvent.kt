package misterbander.crazyeights.net.packets

import misterbander.crazyeights.kryo.KryoPoolable

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

