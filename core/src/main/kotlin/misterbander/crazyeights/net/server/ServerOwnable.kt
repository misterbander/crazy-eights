package misterbander.crazyeights.net.server

interface ServerOwnable : ServerObject
{
	fun setOwner(tabletop: ServerTabletop, ownerUsername: String)
	{
		tabletop.hands[ownerUsername]?.add(this) ?: throw IllegalArgumentException("Can't find hand for user $ownerUsername")
		tabletop.serverObjects.removeValue(this, true)
	}
}
