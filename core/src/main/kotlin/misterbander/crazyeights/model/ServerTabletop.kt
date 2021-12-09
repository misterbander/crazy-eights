package misterbander.crazyeights.model

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*

data class ServerTabletop(
	val users: GdxMap<String, User> = GdxMap(),
	val serverObjects: GdxArray<ServerObject> = GdxArray(),
	val hands: OrderedMap<String, GdxArray<ServerObject>> = OrderedMap(),
	var drawStackHolderId: Int = -1,
	var discardPileHolderId: Int = -1
)
{
	@Transient val idToObjectMap = IntMap<ServerObject>()
	
	@Volatile @Transient var serverObjectsDebugString: String = ""
		private set
	@Volatile @Transient var handsDebugString: String = ""
		private set
	
	fun addServerObject(serverObject: ServerObject, insertAtIndex: Int = -1)
	{
		val idToObjectMap = idToObjectMap
		idToObjectMap[serverObject.id] = serverObject
		if (serverObject is ServerCardGroup)
			serverObject.cards.forEach { idToObjectMap[it.id] = it }
		else if (serverObject is ServerCardHolder)
		{
			idToObjectMap[serverObject.cardGroup.id] = serverObject.cardGroup
			serverObject.cardGroup.cards.forEach { idToObjectMap[it.id] = it }
		}
		if (insertAtIndex != -1)
			serverObjects.insert(insertAtIndex, serverObject)
		else
			serverObjects += serverObject
	}
	
	fun updateDebugStrings()
	{
		val serverObjectsStr = serverObjects.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }
		serverObjectsDebugString = "Server objects (${serverObjects.size}):\n$serverObjectsStr"
		val handsStr = hands.joinToString(separator = "\n") { (key, value) -> "$key: ${value?.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }}" }
		handsDebugString = "Hands:\n$handsStr"
	}
}
