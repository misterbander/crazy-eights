package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.scene2d.Tabletop

@NoArg
data class TouchUpEvent(val username: String, val pointer: Int)

fun Tabletop.onTouchUp(event: TouchUpEvent)
{
	val (username, pointer) = event
	userToCursorsMap[username].remove(pointer)?.remove()
}
