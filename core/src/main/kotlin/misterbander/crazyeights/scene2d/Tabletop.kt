package misterbander.crazyeights.scene2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.plusAssign
import ktx.app.Platform
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
	val userToCursorsMap = GdxMap<String, IntMap<CrazyEightsCursor>>()
	val userToOpponentHandMap = GdxMap<String, OpponentHand>()
	
	val cursors = Group()
	val myCursors = Group()
	val cards = Group()
	val cardHolders = Group()
	val opponentHands = Group()
	val hand = Hand(room)
	
	@Suppress("UNCHECKED_CAST")
	fun setState(state: TabletopState)
	{
		// Add users and cursors
		state.users.forEach { this += it.value }
		
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
			if (ownerUsername == game.user.username)
			{
				for (serverObject: ServerObject in hand!!)
				{
					val gObject = serverObject.toGObject()
					gObject.getModule<Ownable>()?.wasInHand = true
					this.hand += gObject as Groupable<CardGroup>
				}
			}
			else
			{
				val opponentHand = userToOpponentHandMap[ownerUsername]!!
				for (serverObject: ServerObject in hand!!)
				{
					val gObject = serverObject.toGObject()
					opponentHand += gObject as Groupable<CardGroup>
				}
				opponentHand.arrange()
			}
		}
		
		arrangePlayers()
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
			val (id, x, y, rotation, spreadSeparation, spreadCurvature, serverCards, type, lockHolder) = this
			val cards = GdxArray<Groupable<CardGroup>>()
			for ((cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp) in serverCards)
			{
				val card = Card(room, cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp)
				idToGObjectMap[cardId] = card
				cards += card
			}
			val cardGroup = CardGroup(room, id, x, y, rotation, spreadSeparation, spreadCurvature, cards, type, lockHolder)
			idToGObjectMap[id] = cardGroup
			cardGroup
		}
		is ServerCardHolder ->
		{
			val (id, x, y, rotation, serverCardGroup, lockHolder) = this
			val (cardGroupId, _, _, _, spreadSeparation, spreadCurvature, serverCards, type) = serverCardGroup
			val cards = GdxArray<Groupable<CardGroup>>()
			for ((cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp) in serverCards)
			{
				val card = Card(room, cardId, cardX, cardY, cardRotation, rank, suit, isFaceUp)
				idToGObjectMap[cardId] = card
				cards += card
			}
			val cardGroup = CardGroup(
				room,
				cardGroupId,
				spreadSeparation = spreadSeparation,
				spreadCurvature = spreadCurvature,
				cards = cards,
				type = type
			)
			idToGObjectMap[cardGroupId] = cardGroup
			val cardHolder = CardHolder(room, id, x, y, rotation, cardGroup, lockHolder = lockHolder)
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
		userToCursorsMap[user.username] = IntMap<CrazyEightsCursor>().apply { this[-1] = cursor }
		room.addUprightGObject(cursor)
		if (user != game.user)
		{
			cursors += cursor
			opponentHands += userToOpponentHandMap.getOrPut(user.username) { OpponentHand(room, user = user) }
		}
		else if (Platform.isMobile)
		{
			cursors += cursor
			userToCursorsMap[game.user.username]!![0] = cursor
		}
	}
	
	operator fun minusAssign(user: User)
	{
		users.remove(user.username)
		userToCursorsMap.remove(user.username)?.apply { values().forEach { it.remove() } }
		userToOpponentHandMap[user.username]!!.remove()
		for (gObject: GObject<CrazyEights> in idToGObjectMap.values())
		{
			val lockable = gObject.getModule<Lockable>()
			if (lockable != null && lockable.lockHolder == user)
				lockable.unlock()
		}
	}
	
	fun arrangePlayers()
	{
		users.orderedKeys().forEachIndexed { index, username ->
			val opponentHand = userToOpponentHandMap[username] ?: return@forEachIndexed
			val radius = 432F
			val directionToPlayer = -90 - 360F*index/users.size
			opponentHand.realX = 640 + radius*MathUtils.cosDeg(directionToPlayer)
			opponentHand.realY = 360 + radius*MathUtils.sinDeg(directionToPlayer)
			opponentHand.rotation = directionToPlayer + 90
		}
		val myIndex = users.orderedKeys().indexOf(game.user.username)
		room.cameraAngleInterpolator.target = 360F*myIndex/users.size
	}
	
	fun hitDragTarget(x: Float, y: Float): DragTarget? = hitDragTarget(cards, x, y) ?: hitDragTarget(cardHolders, x, y)
	
	private fun hitDragTarget(group: Group, x: Float, y: Float): DragTarget?
	{
		if (group.touchable == Touchable.disabled)
			return null
		if (!group.isVisible)
			return null
		val point = tempVec
		val childrenArray = group.children
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
		users.clear()
		idToGObjectMap.clear()
		userToCursorsMap.clear()
		userToOpponentHandMap.clear()
		myCursors.clear()
		
		hand.reset()
		cursors.clearChildren()
		cards.clearChildren()
		cardHolders.clearChildren()
		opponentHands.clearChildren()
	}
}
