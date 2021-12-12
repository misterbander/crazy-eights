package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.Align
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
import misterbander.crazyeights.model.ServerTabletop
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class Tabletop(val room: Room)
{
	val game: CrazyEights
		get() = room.game
	
	val users = GdxMap<String, User>()
	val idToGObjectMap = IntMap<GObject<CrazyEights>>()
	val userToCursorsMap = GdxMap<String, IntMap<CrazyEightsCursor>>()
	val userToHandMap = OrderedMap<String, Hand>()
	
	val playDirectionIndicator = PlayDirectionIndicator(room)
	val cursors = Group()
	val myCursors = Group()
	val cards = Group()
	val cardHolders = Group()
	val opponentHands = Group()
	val myHand = MyHand(room)
	val powerCardEffects = Group()
	val persistentPowerCardEffects = Group()
	
	val drawStackHolder: CardHolder?
		get() = cardHolders.children.firstOrNull { (it as? CardHolder)?.defaultType == ServerCardGroup.Type.STACK } as? CardHolder
	val drawStack: CardGroup?
		get() = drawStackHolder?.cardGroup
	val discardPileHolder: CardHolder?
		get() = cardHolders.children.firstOrNull { (it as? CardHolder)?.defaultType == ServerCardGroup.Type.PILE } as? CardHolder
	val discardPile: CardGroup?
		get() = discardPileHolder?.cardGroup
	
	var suitChooser: SuitChooser? = null
	var isPowerCardJustPlayed = false
	
	@Suppress("UNCHECKED_CAST")
	fun setState(state: ServerTabletop)
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
			if (ownerUsername == game.user.name)
			{
				userToHandMap[game.user.name] = myHand
				for (serverObject: ServerObject in hand!!)
				{
					val gObject = serverObject.toGObject()
					gObject.getModule<Ownable>()?.wasInHand = true
					this.myHand += gObject as Groupable<CardGroup>
				}
			}
			else
			{
				val opponentHand = userToHandMap.getOrPut(ownerUsername) {
					OpponentHand(room, user = users[ownerUsername] ?: User(ownerUsername, Color.LIGHT_GRAY))
				}
				opponentHands += opponentHand
				for (serverObject: ServerObject in hand!!)
				{
					val gObject = serverObject.toGObject()
					opponentHand += gObject as Groupable<CardGroup>
				}
				opponentHand.arrange()
			}
		}
		
		val drawStackHolder = drawStackHolder!!
		room.passButton.setPosition(drawStackHolder.x, drawStackHolder.y, Align.center)
		
		arrangePlayers()
	}
	
	private fun ServerObject.toGObject(): GObject<CrazyEights> = when (this)
	{
		is ServerCard ->
		{
			val (id, x, y, rotation, rank, suit, isFaceUp, lockHolder) = this
			val card = Card(room, id, x, y, rotation, rank, suit, isFaceUp, lockHolder?.let { users[it] ?: User(it) })
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
			val cardGroup = CardGroup(room, id, x, y, rotation, spreadSeparation, spreadCurvature, cards, type, lockHolder?.let { users[it] ?: User(it) })
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
			val cardHolder = CardHolder(room, id, x, y, rotation, cardGroup, lockHolder = lockHolder?.let { users[it] ?: User(it) })
			idToGObjectMap[id] = cardHolder
			cardHolders += cardHolder
			cardHolder
		}
		else -> throw NotImplementedError("No implementation for $this")
	}
	
	operator fun plusAssign(user: User)
	{
		users[user.name] = user
		if (user.isAi)
			return
		val cursor = CrazyEightsCursor(room, user, user == game.user)
		userToCursorsMap[user.name] = IntMap<CrazyEightsCursor>().apply { this[-1] = cursor }
		room.addUprightGObject(cursor)
		if (user != game.user)
			cursors += cursor
		else if (Platform.isMobile)
		{
			cursors += cursor
			userToCursorsMap[game.user.name]!![0] = cursor
		}
	}
	
	operator fun minusAssign(user: User)
	{
		users.remove(user.name)
		userToCursorsMap.remove(user.name)?.apply { values().forEach { it.remove() } }
		val hand = userToHandMap[user.name]!!
		if (hand.cardGroup.cards.isEmpty && (!room.isGameStarted || user.name !in room.gameState!!.players))
		{
			userToHandMap.remove(user.name)
			hand.remove()
		}
		else if (hand is OpponentHand)
			hand.user = hand.user.copy(color = Color.LIGHT_GRAY)
		for (gObject: GObject<CrazyEights> in idToGObjectMap.values())
		{
			val lockable = gObject.getModule<Lockable>()
			if (lockable != null && lockable.lockHolder == user)
				lockable.unlock(false)
		}
	}
	
	fun arrangePlayers()
	{
		userToHandMap.orderedKeys().forEachIndexed { index, username ->
			val hand = userToHandMap[username] as? OpponentHand ?: return@forEachIndexed
			val radius = 432F
			val directionToPlayer = -90 - 360F*index/userToHandMap.size
			hand.realX = 640 + radius*MathUtils.cosDeg(directionToPlayer)
			hand.realY = 360 + radius*MathUtils.sinDeg(directionToPlayer)
			hand.rotation = directionToPlayer + 90
		}
		val myIndex = userToHandMap.orderedKeys().indexOf(game.user.name)
		room.cameraAngle = 360F*myIndex/userToHandMap.size
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
		userToHandMap.clear()
		myCursors.clear()
		
		playDirectionIndicator.reset()
		cursors.clearChildren()
		cards.clearChildren()
		cardHolders.clearChildren()
		opponentHands.clearChildren()
		myHand.reset()
		powerCardEffects.clearChildren()
		persistentPowerCardEffects.clearChildren()
		
		suitChooser = null
	}
}
