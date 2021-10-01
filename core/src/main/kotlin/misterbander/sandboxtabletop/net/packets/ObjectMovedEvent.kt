package misterbander.sandboxtabletop.net.packets

import misterbander.sandboxtabletop.net.KryoPoolable

data class ObjectMovedEvent(
	override var seqNumber: Int = -1,
	var id: Int = -1,
	var x: Float = 0F,
	var y: Float = 0F,
) : KryoPoolable, Acknowledgeable
{
	override fun reset()
	{
		id = -1
		x = 0F
		y = 0F
	}
}
