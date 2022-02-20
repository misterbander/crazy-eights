package misterbander.crazyeights.model

import ktx.collections.*
import misterbander.crazyeights.net.ServerTabletop

interface ServerObject
{
	val id: Int
	var x: Float
	var y: Float
	var rotation: Float
	
	fun setPosition(x: Float, y: Float)
	{
		this.x = x
		this.y = y
	}
	
	fun toFront(tabletop: ServerTabletop)
	{
		// Remove the object and add it again to move it to the front
		if (tabletop.serverObjects.removeValue(this, true))
			tabletop.serverObjects += this
	}
}
