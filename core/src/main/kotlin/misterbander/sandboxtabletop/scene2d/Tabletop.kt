package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import ktx.collections.GdxMap
import ktx.collections.GdxSet
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import ktx.collections.set
import misterbander.gframework.scene2d.plusAssign
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User

class Tabletop(private val screen: RoomScreen)
{
	private val users = GdxSet<User>()
	
	val userCursorMap = GdxMap<String, SandboxTabletopCursor>()
	val cursors = Group()
	var myCursor: SandboxTabletopCursor? = null
	
	fun setState(state: TabletopState)
	{
		state.users.forEach { this += it }
		myCursor?.toFront()
	}
	
	operator fun plusAssign(user: User)
	{
		users += user
		val cursor = SandboxTabletopCursor(screen, user, user == screen.game.user)
		userCursorMap[user.username] = cursor
		if (user != screen.game.user)
			cursors += cursor
		else if (Gdx.app.type != Application.ApplicationType.Desktop)
		{
			cursors += cursor
			myCursor = cursor
		}
	}
	
	operator fun minusAssign(user: User)
	{
		users -= user
		userCursorMap.remove(user.username)?.remove()
	}
	
	fun reset()
	{
		users.clear()
		
		userCursorMap.clear()
		cursors.clearChildren()
		myCursor = null
	}
}
