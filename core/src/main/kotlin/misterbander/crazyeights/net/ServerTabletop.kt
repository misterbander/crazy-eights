package misterbander.crazyeights.net

import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.*
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User

class ServerTabletop(val parent: CrazyEightsServer, val drawStackHolder: ServerCardHolder, val discardPileHolder: ServerCardHolder)
{
	val idToObjectMap = IntMap<ServerObject>()
	
	val users: GdxMap<String, User> = GdxMap()
	val hands: OrderedMap<String, GdxArray<ServerObject>> = OrderedMap()
	
	val serverObjects: GdxArray<ServerObject> = GdxArray()
	
	var suitChooser: String? = null
	
	@Volatile var serverObjectsDebugString: String = ""
		private set
	@Volatile var handsDebugString: String = ""
		private set
	
	fun addServerObject(serverObject: ServerObject, insertAtIndex: Int = -1)
	{
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
	
	fun toTabletopState(): TabletopState = TabletopState(users, serverObjects, hands)
}
