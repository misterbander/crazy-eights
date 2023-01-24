package misterbander.crazyeights.net.server

import ktx.collections.*

interface ServerOwnable : ServerLockable
{
	var lastOwner: String?
	
	fun getOwner(tabletop: ServerTabletop): String? = tabletop.hands.firstOrNull { this in it.value }?.key
	
	fun setOwner(tabletop: ServerTabletop, ownerUsername: String?)
	{
		val currentOwner = getOwner(tabletop)
		val newOwnerHand = if (ownerUsername != null)
			tabletop.hands[ownerUsername] ?: throw IllegalArgumentException("Can't find hand for user $ownerUsername")
		else
			null
		if (currentOwner != null)
		{
			tabletop.hands[currentOwner]!!.removeValue(this, true)
			lastOwner = currentOwner
		}
		if (newOwnerHand != null)
		{
			newOwnerHand += this
			tabletop.serverObjects.removeValue(this, true)
		}
		else
			tabletop.serverObjects += this
	}
}
