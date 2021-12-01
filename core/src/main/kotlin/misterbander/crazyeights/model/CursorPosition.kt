package misterbander.crazyeights.model

import misterbander.crazyeights.net.KryoPoolable

data class CursorPosition(
	var username: String = "",
	var x: Float = 0F,
	var y: Float = 0F,
	var pointer: Int = -1
): KryoPoolable
{
	override fun reset()
	{
		username = ""
		x = 0F
		y = 0F
		pointer = -1
	}
}
