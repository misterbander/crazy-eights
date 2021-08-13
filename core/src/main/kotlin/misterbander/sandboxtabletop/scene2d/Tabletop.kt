package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.Group
import ktx.actors.plusAssign
import ktx.collections.GdxMap
import ktx.collections.set

class Tabletop
{
	val userCursorMap = GdxMap<String, SandboxTabletopCursor>()
	val cursors = Group()
	var myCursor: SandboxTabletopCursor? = null
	
	fun addCursor(username: String, cursor: SandboxTabletopCursor)
	{
		userCursorMap[username] = cursor
		cursors += cursor
	}
	
	fun removeCursor(username: String) = userCursorMap.remove(username)?.remove()
	
	fun reset()
	{
		userCursorMap.clear()
		cursors.clearChildren()
		myCursor = null
	}
}
