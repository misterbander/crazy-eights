package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.plusAssign
import ktx.collections.GdxMap
import ktx.collections.gdxArrayOf
import ktx.collections.plusAssign
import ktx.collections.set
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.ServerCardGroup
import misterbander.sandboxtabletop.model.ServerObject
import misterbander.sandboxtabletop.model.TabletopState
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.scene2d.modules.Lockable

class Tabletop(private val room: Room)
{
	val game: SandboxTabletop
		get() = room.game
	
	val users = OrderedMap<String, User>()
	val idToGObjectMap = IntMap<GObject<SandboxTabletop>>()
	val userToCursorMap = GdxMap<String, SandboxTabletopCursor>()
	
	val cursors = Group()
	var myCursor: SandboxTabletopCursor? = null // TODO Make responsive
	val cards = Group()
	
	fun setState(state: TabletopState)
	{
		// Add users and cursors
		state.users.forEach { this += it.value }
		myCursor?.toFront()
		
		// Add server objects
		state.serverObjects.forEach { serverObject: ServerObject ->
			when (serverObject)
			{
				is ServerCard ->
				{
					val (id, x, y, rotation, rank, suit, isFaceUp, lockHolder) = serverObject
					val card = Card(room, id, x, y, rotation, rank, suit, isFaceUp, lockHolder)
					idToGObjectMap[id] = card
					cards += card
				}
				is ServerCardGroup ->
				{
					val (id, x, y, rotation, serverCards, type, lockHolder) = serverObject
					val cards = gdxArrayOf<Card>()
					serverCards.forEach { serverCard: ServerCard ->
						val (cardId, _, _, _, rank, suit, isFaceUp) = serverCard
						val card = Card(room, cardId, 0F, 0F, 0F, rank, suit, isFaceUp)
						idToGObjectMap[cardId] = card
						cards += card
					}
					val cardGroup = CardGroup(room, id, x, y, rotation, cards, type, lockHolder)
					idToGObjectMap[id] = cardGroup
					this.cards += cardGroup
				}
			}
		}
	}
	
	operator fun plusAssign(user: User)
	{
		users[user.username] = user
		val cursor = SandboxTabletopCursor(room, user, user == game.user)
		userToCursorMap[user.username] = cursor
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
		userToCursorMap.remove(user.username)?.remove()
		idToGObjectMap.values().forEach { gObject: GObject<SandboxTabletop> ->
			val lockable = gObject.getModule<Lockable>()
			if (lockable != null && lockable.isLockHolder)
				lockable.unlock()
		}
	}
	
	fun hitDragTarget(x: Float, y: Float): DragTarget? = hitDragTarget(cards, x, y)
	
	private fun hitDragTarget(group: Group, x: Float, y: Float): DragTarget?
	{
		if (group.touchable == Touchable.disabled)
			return null
		if (!group.isVisible)
			return null
		val point = tempVec
		val childrenArray = group.children.items
		for (i in group.children.size - 1 downTo 0)
		{
			val child = childrenArray[i]
			child.parentToLocalCoordinates(point.set(x, y))
			val hit = child.hit(point.x, point.y, true)
			if (hit is DragTarget && !hit.lockable.isLocked)
				return if (hit is Card && hit.cardGroup != null) hit.cardGroup else hit
		}
		return null
	}
	
	fun reset()
	{
		idToGObjectMap.clear()
		
		users.clear()
		userToCursorMap.clear()
		cursors.clearChildren()
		myCursor = null
		cards.clearChildren()
	}
}
