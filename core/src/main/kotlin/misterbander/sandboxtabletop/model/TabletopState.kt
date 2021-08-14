package misterbander.sandboxtabletop.model

import ktx.collections.GdxSet

data class TabletopState(
	val users: GdxSet<User> = GdxSet()
)
