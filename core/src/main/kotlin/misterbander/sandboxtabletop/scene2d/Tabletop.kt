package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.GdxMap
import ktx.collections.set
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.plusAssign
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.ServerObject
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.scene2d.modules.Lockable

class Tabletop(private val room: Room)
{
	val game: SandboxTabletop
		get() = room.game
	
	val idGObjectMap = IntMap<GObject<SandboxTabletop>>()
	
	val users = OrderedMap<String, User>()
	val userCursorMap = GdxMap<String, SandboxTabletopCursor>()
	val cursors = Group()
	var myCursor: SandboxTabletopCursor? = null
	val cards = Group()
	
	fun setState(state: TabletopState)
	{
		// Add users and cursors
		state.users.forEach { this += it.value }
		myCursor?.toFront()
		
		// Add server objects
		state.serverObjects.forEach { serverObject: ServerObject ->
			if (serverObject is ServerCard)
			{
				val (id, x, y, rotation, rank, suit, isFaceUp, lockHolder) = serverObject
				val card = Card(room, id, x, y, rotation, rank, suit, isFaceUp, lockHolder)
				idGObjectMap[id] = card
				cards += card
			}
		}
	}
	
	operator fun plusAssign(user: User)
	{
		users[user.username] = user
		val cursor = SandboxTabletopCursor(room, user, user == game.user)
		userCursorMap[user.username] = cursor
		if (user != game.user)
			cursors += cursor
		else if (Gdx.app.type != Application.ApplicationType.Desktop)
		{
			cursors += cursor
			myCursor = cursor
		}
	}
	
	operator fun minusAssign(user: User)
	{
		users.remove(user.username)
		userCursorMap.remove(user.username)?.remove()
		idGObjectMap.values().forEach { gObject: GObject<SandboxTabletop> ->
			val lockable = gObject.getModule<Lockable>()
			if (lockable != null && lockable.isLockHolder)
				lockable.unlock()
		}
	}
	
	fun reset()
	{
		idGObjectMap.clear()
		
		users.clear()
		userCursorMap.clear()
		cursors.clearChildren()
		myCursor = null
		cards.clearChildren()
	}
}
