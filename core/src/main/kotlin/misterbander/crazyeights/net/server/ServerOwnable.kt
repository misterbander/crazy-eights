package misterbander.crazyeights.net.server

interface ServerOwnable : ServerObject
{
	fun setOwner(tabletop: ServerTabletop, ownerUsername: String)
	{
		tabletop.serverObjects.removeValue(this, true)
		tabletop.hands[ownerUsername]!!.add(this)
	}
}
