package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.plusAssign
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.model.TabletopState
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class Tabletop(private val room: Room)
{
	val game: CrazyEights
		get() = room.game
	
	val users = OrderedMap<String, User>()
	val idToGObjectMap = IntMap<GObject<CrazyEights>>()
	val userToCursorMap = GdxMap<String, CrazyEightsCursor>()
	
	val cursors = Group()
	var myCursor: CrazyEightsCursor? = null // TODO Make responsive
	val cards = Group()
	val cardHolders = Group()
	val hand = Hand(room, GdxArray())
	val opponentHands = GdxMap<String, GdxArray<GObject<CrazyEights>>>()
	
	fun setState(state: TabletopState)
	{
		// Add users and cursors
		state.users.forEach { this += it.value }
		myCursor?.toFront()
		
		// Add server objects
		for (serverObject: ServerObject in state.serverObjects)
		{
			val gObject = serverObject.toGObject()
			if (gObject is CardHolder)
				cardHolders += gObject
			else
				cards += gObject
		}
		
		// Add each hand
		for ((ownerUsername, hand) in state.hands)
		{
			for (serverObject: ServerObject in hand!!)
			{
				val gObject = serverObject.toGObject()
				if (ownerUsername == game.user.username)
				{
					gObject.getModule<Ownable>()?.wasInHand = true
					this.hand += gObject
				}
				else
				{
					gObject.isVisible = false
					opponentHands.getOrPut(ownerUsername) { GdxArray() } += gObject
					cards += gObject
				}
			}
		}
	}
	
	private fun ServerObject.toGObject(): GObject<CrazyEights> = when (this)
	{
		is ServerCard ->
		{
			val (id, x, y, rotation, rank, suit, isFaceUp, lockHolder) = this
			val card = Card(room, id, x, y, rotation, rank, suit, isFaceUp, lockHolder)
			idToGObjectMap[id] = card
			card
		}
		is ServerCardGroup ->
		{
			val (id, x, y, rotation, serverCards, type, lockHolder) = this
			val cards = GdxArray<Card>()
			for (serverCard: ServerCard in serverCards)
			{
				val (cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp) = serverCard
				val card = Card(room, cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp)
				idToGObjectMap[cardId] = card
				cards += card
			}
			val cardGroup = CardGroup(room, id, x, y, rotation, cards, type, lockHolder)
			idToGObjectMap[id] = cardGroup
			cardGroup
		}
		is ServerCardHolder ->
		{
			val (id, x, y, rotation, serverCardGroup, lockHolder) = this
			val (cardGroupId, _, _, _, serverCards, type) = serverCardGroup
			val cards = GdxArray<Card>()
			for (serverCard: ServerCard in serverCards)
			{
				val (cardId, _, _, _, rank, suit, isFaceUp) = serverCard
				val card = Card(room, cardId, 0F, 0F, 0F, rank, suit, isFaceUp)
				idToGObjectMap[cardId] = card
				cards += card
			}
			val cardGroup = CardGroup(room, cardGroupId, 0F, 0F, 0F, cards, type)
			idToGObjectMap[cardGroupId] = cardGroup
			val cardHolder = CardHolder(room, id, x, y, rotation, cardGroup, lockHolder)
			idToGObjectMap[id] = cardHolder
			cardHolders += cardHolder
			cardHolder
		}
		else -> throw NotImplementedError("No implementation for $this")
	}
	
	operator fun plusAssign(user: User)
	{
		users[user.username] = user
		val cursor = CrazyEightsCursor(room, user, user == game.user)
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
		for (gObject: GObject<CrazyEights> in idToGObjectMap.values())
		{
			val lockable = gObject.getModule<Lockable>()
			if (lockable != null && lockable.lockHolder == user)
				lockable.unlock()
		}
	}
	
	fun hitDragTarget(x: Float, y: Float): DragTarget? = hitDragTarget(cards, x, y) ?: hitDragTarget(cardHolders, x, y)
	
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
			if (hit is DragTarget && hit.lockable?.isLocked != true)
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
		cardHolders.clearChildren()
		hand.reset()
		opponentHands.clear()
	}
}
