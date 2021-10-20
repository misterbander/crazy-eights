package misterbander.crazyeights.model

import misterbander.crazyeights.net.KryoPoolable

data class CursorPosition(var username: String = "", var x: Float = 0F, var y: Float = 0F): KryoPoolable
{
	override fun reset()
	{
		username = ""
		x = 300F
		y = 0F
	}
}