package misterbander.crazyeights.model

import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*

data class TabletopState(
	val users: GdxMap<String, User> = GdxMap(),
	val serverObjects: GdxArray<ServerObject> = GdxArray(),
	val hands: OrderedMap<String, GdxArray<ServerObject>> = OrderedMap(),
	var isGameStarted: Boolean = false
)
{
	@Volatile @Transient var serverObjectsDebugString: String = ""
		private set
	@Volatile @Transient var handsDebugString: String = ""
		private set
	
	fun updateDebugStrings()
	{
		val serverObjectsStr = serverObjects.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }
		serverObjectsDebugString = "Server objects (${serverObjects.size}):\n$serverObjectsStr"
		val handsStr = hands.joinToString(separator = "\n") { (key, value) -> "$key: ${value?.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }}" }
		handsDebugString = "Hands:\n$handsStr"
	}
}
