package misterbander.crazyeights.model

import misterbander.crazyeights.net.ServerTabletop

interface ServerOwnable : ServerObject
{
	fun setOwner(tabletop: ServerTabletop, ownerUsername: String)
	{
		tabletop.serverObjects.removeValue(this, true)
		tabletop.hands[ownerUsername]!!.add(this)
	}
}
