package misterbander.crazyeights.net.packets

import misterbander.crazyeights.kryo.KryoPoolable
import misterbander.crazyeights.kryo.cursorPositionPool

data class CursorPosition(
	var username: String = "",
	var x: Float = 0F,
	var y: Float = 0F,
	var pointer: Int = -1
) : KryoPoolable
{
	override fun free() = cursorPositionPool.free(this)
	
	override fun reset()
	{
		username = ""
		x = 0F
		y = 0F
		pointer = -1
	}
}
