package misterbander.sandboxtabletop.net.packets

import ktx.collections.GdxSet
import misterbander.sandboxtabletop.model.User

data class RoomState(val users: GdxSet<User>)
{
	@Suppress("UNUSED")
	private constructor() : this(GdxSet())
}
