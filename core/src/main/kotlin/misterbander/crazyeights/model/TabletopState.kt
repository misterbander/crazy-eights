package misterbander.crazyeights.model

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*

data class TabletopState(
	val users: OrderedMap<String, User> = OrderedMap(),
	val serverObjects: GdxArray<ServerObject> = GdxArray(),
	val hands: GdxMap<String, GdxArray<ServerObject>> = GdxMap()
)
