package misterbander.crazyeights.model

import ktx.collections.*

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
	
	fun toFront(state: ServerTabletop)
	{
		// Remove the object and add it again to move it to the front
		if (state.serverObjects.removeValue(this, true))
			state.serverObjects += this
	}
	
	fun setOwner(ownerUsername: String, state: ServerTabletop)
	{
		state.serverObjects.removeValue(this, true)
		state.hands[ownerUsername]!!.add(this)
	}
}
