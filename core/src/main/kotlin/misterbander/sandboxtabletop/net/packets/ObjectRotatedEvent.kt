package misterbander.sandboxtabletop.net.packets

import misterbander.sandboxtabletop.net.KryoPoolable

data class ObjectRotatedEvent(
	override var seqNumber: Int = -1,
	var id: Int = -1,
	var rotation: Float = 0F,
) : KryoPoolable, Acknowledgeable
{
	override fun reset()
	{
		id = -1
		rotation = 0F
	}
}
