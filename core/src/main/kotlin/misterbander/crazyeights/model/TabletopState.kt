package misterbander.crazyeights.model

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*

data class TabletopState(
	val users: GdxMap<String, User> = GdxMap(),
	val serverObjects: GdxArray<ServerObject> = GdxArray(),
	val hands: OrderedMap<String, GdxArray<ServerObject>> = OrderedMap()
)
