package misterbander.crazyeights.model

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.GdxArray

data class TabletopState(
	val users: OrderedMap<String, User> = OrderedMap(),
	val serverObjects: GdxArray<ServerObject> = GdxArray()
)
