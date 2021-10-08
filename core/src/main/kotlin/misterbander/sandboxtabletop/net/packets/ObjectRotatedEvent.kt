package misterbander.sandboxtabletop.net.packets

import misterbander.sandboxtabletop.net.KryoPoolable

data class ObjectRotatedEvent(
	var id: Int = -1,
	var rotation: Float = 0F,
	var rotatorUsername: String = ""
) : KryoPoolable
{
	override fun reset()
	{
		id = -1
		rotation = 0F
		rotatorUsername = ""
	}
}
