package misterbander.crazyeights.net.packets

import misterbander.crazyeights.Room
import misterbander.crazyeights.model.NoArg

@NoArg
data class TouchUpEvent(val username: String, val pointer: Int)

fun Room.onTouchUp(event: TouchUpEvent)
{
	val (username, pointer) = event
	tabletop.userToCursorsMap[username].remove(pointer)?.remove()
}
