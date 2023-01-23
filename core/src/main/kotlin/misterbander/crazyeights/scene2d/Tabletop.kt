package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.along
import ktx.actors.alpha
import ktx.actors.onChange
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.app.Platform
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.net.packets.*
import misterbander.crazyeights.net.server.*
import misterbander.crazyeights.net.server.game.Ruleset
import misterbander.crazyeights.scene2d.actions.*
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec
import kotlin.math.round

class Tabletop(val room: RoomScreen) : Group()
{
	private val game: CrazyEights
		get() = room.game
	
	val idToGObjectMap = IntMap<GObject<CrazyEights>>()
	
	private val users = GdxMap<String, User>()
	private val userToCursorsMap = GdxMap<String, IntMap<CrazyEightsCursor>>()
	private val userToHandMap = OrderedMap<String, Hand>()
	val userCount: Int
		get() = users.size
	
	private val playDirectionIndicator = PlayDirectionIndicator(room)
	private val cursors = Group()
	private val myCursors = Group()
	private val cursorPositions = IntMap<CursorPosition>()
	val cards = Group()
	private val cardHolders = Group()
	private val opponentHands = Group()
	val myHand = MyHand(room)
	private val powerCardEffects = Group()
	val persistentPowerCardEffects = Group()
	val passButton = scene2d.textButton("Pass") {
		setOrigin(Align.center)
		isTransform = true
		isVisible = false
		room.addUprightGObject(this)
		onChange {
			room.click.play()
			game.client?.sendTCP(PassEvent)
			isVisible = false
		}
	}
	
	val drawStackHolder: CardHolder
		get() = cardHolders.children.first { (it as? CardHolder)?.defaultType == ServerCardGroup.Type.STACK } as CardHolder
	val drawStack: CardGroup?
		get() = drawStackHolder.cardGroup
	val discardPileHolder: CardHolder
		get() = cardHolders.children.first { (it as? CardHolder)?.defaultType == ServerCardGroup.Type.PILE } as CardHolder
	val discardPile: CardGroup?
		get() = discardPileHolder.cardGroup
	
	var ruleset = Ruleset(firstDiscardOnDealTriggersPower = true)
		private set
	var gameState: GameState? = null
		private set
	val isGameStarted: Boolean
		get() = gameState != null
	private var suitChooser: SuitChooser? = null
	var isPowerCardJustPlayed = false
	
	init
	{
		this += playDirectionIndicator
		this += cardHolders
		this += opponentHands
		this += cards
		this += myHand
		this += powerCardEffects
		this += persistentPowerCardEffects
		this += passButton
		this += cursors
		this += myCursors
	}
	
	@Suppress("UNCHECKED_CAST")
	fun setState(state: TabletopState)
	{
		val (users, serverObjects, hands) = state
		
		// Add users and cursors
		users.forEach { addUser(it.value) }
		
		// Add server objects
		for (serverObject: ServerObject in serverObjects)
		{
			val gObject = serverObject.toGObject()
			if (gObject is CardHolder)
				cardHolders += gObject
			else
				cards += gObject
		}
		
		// Add each hand
		for ((ownerUsername, hand) in hands)
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
		
		passButton.setPosition(drawStackHolder.x, drawStackHolder.y, Align.center)
		
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
			val cardGroup = CardGroup(
				room,
				id,
				x,
				y,
				rotation,
				spreadSeparation,
				spreadCurvature,
				cards,
				type,
				lockHolder?.let { users[it] ?: User(it) })
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
			val cardHolder =
				CardHolder(room, id, x, y, rotation, cardGroup, lockHolder = lockHolder?.let { users[it] ?: User(it) })
			idToGObjectMap[id] = cardHolder
			cardHolders += cardHolder
			cardHolder
		}
		else -> throw NotImplementedError("No implementation for $this")
	}
	
	fun renderCursors(shouldSyncServer: Boolean)
	{
		for (i in 0..19)
		{
			if (Gdx.input.isTouched(i) || Platform.isDesktop && i == 0)
			{
				val (inputX, inputY) = stage.screenToStageCoordinates(
					tempVec.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
				)
				if (!Platform.isDesktop || i != 0)
				{
					val cursorsMap: IntMap<CrazyEightsCursor>? = userToCursorsMap[game.user.name]
					cursorsMap?.getOrPut(i) {
						val cursor = CrazyEightsCursor(room, game.user, true)
						cursorsMap[i] = cursor
						cursors += cursor
						room.addUprightGObject(cursor)
						cursor
					}?.snapPosition(inputX, inputY)
				}
				
				if (shouldSyncServer)
				{
					val cursorPosition = cursorPositions.getOrPut(i) { CursorPosition() }
					if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
					{
						cursorPosition.apply {
							username = game.user.name
							x = inputX
							y = inputY
							pointer = i
						}
						game.client?.sendTCP(cursorPosition)
					}
				}
			}
			else if (i in cursorPositions && cursorPositions.size > 1)
			{
				cursorPositions.remove(i)
				userToCursorsMap[game.user.name]?.remove(i)?.remove()
				game.client?.sendTCP(TouchUpEvent(game.user.name, i))
			}
		}
	}
	
	private fun addUser(user: User)
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
	
	private fun removeUser(user: User)
	{
		users.remove(user.name)
		userToCursorsMap.remove(user.name)?.apply { values().forEach { it.remove() } }
		val hand = userToHandMap[user.name]!!
		if (hand.cardGroup.cards.isEmpty && (!isGameStarted || user.name !in gameState!!.players) || user.isAi)
		{
			userToHandMap.remove(user.name)
			hand.remove()
			if (user.isAi)
			{
				for (groupable: Groupable<CardGroup> in GdxArray(hand.cardGroup.cards))
				{
					if (groupable is Card)
					{
						groupable.cardGroup = null
						groupable.isFaceUp = true
					}
				}
			}
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
	
	private fun arrangePlayers()
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
	
	fun onUserJoined(event: UserJoinedEvent)
	{
		val user = event.user
		if (user != game.user)
		{
			addUser(user)
			val opponentHand = userToHandMap.getOrPut(user.name) {
				OpponentHand(room)
			} as OpponentHand
			opponentHand.user = user
			opponentHands += opponentHand
		}
		if (!user.isAi)
			room.chatBox.chat("${user.name} joined the game", Color.YELLOW)
		arrangePlayers()
	}
	
	fun onUserLeft(event: UserLeftEvent)
	{
		val user = event.user
		removeUser(user)
		if (!user.isAi)
			room.chatBox.chat("${user.name} left the game", Color.YELLOW)
		arrangePlayers()
		if (user == room.userDialog.user)
			room.userDialog.hide()
	}
	
	fun onSwapSeats(event: SwapSeatsEvent)
	{
		val (user1, user2) = event
		val keys: GdxArray<String> = userToHandMap.orderedKeys()
		val index1 = keys.indexOf(user1, false)
		val index2 = keys.indexOf(user2, false)
		keys.swap(index1, index2)
		arrangePlayers()
	}
	
	fun onCursorMoved(cursorPosition: CursorPosition)
	{
		val (username, x, y, pointer) = cursorPosition
		val cursorsMap = userToCursorsMap[username]!!
		cursorsMap[-1]?.remove()
		if (pointer in cursorsMap)
			cursorsMap[pointer]!!.setPosition(x, y)
		else
		{
			val cursor = CrazyEightsCursor(room, users[username]!!)
			cursorsMap[pointer] = cursor
			cursors += cursor
			room.addUprightGObject(cursor)
			cursor.snapPosition(x, y)
		}
		cursorPosition.free()
	}
	
	fun onTouchUp(event: TouchUpEvent)
	{
		val (username, pointer) = event
		userToCursorsMap[username].remove(pointer)?.remove()
	}
	
	fun onObjectLock(event: ObjectLockEvent)
	{
		val (id, lockerUsername) = event
		val toLock = idToGObjectMap[id]!!
		toLock.getModule<Lockable>()?.lock(users[lockerUsername]!!)
		
		if (isGameStarted && toLock is Card && toLock.cardGroup == drawStack)
			gameState!!.drawCount++
	}
	
	fun onObjectUnlock(event: ObjectUnlockEvent)
	{
		idToGObjectMap[event.id]?.getModule<Lockable>()?.unlock()
	}
	
	@Suppress("UNCHECKED_CAST")
	fun onObjectOwn(event: ObjectOwnEvent)
	{
		val (id, ownerUsername) = event
		val toOwn = idToGObjectMap[id] as Groupable<CardGroup>
		val hand = userToHandMap[ownerUsername]!!
		toOwn.getModule<Lockable>()?.unlock()
		(toOwn.parent as? CardGroup)?.minusAssign(toOwn)
		hand += toOwn
		if (hand is MyHand)
		{
			toOwn.getModule<Ownable>()?.wasInHand = true
			if (toOwn is Card)
				toOwn.isFaceUp = true
			hand.arrange(false)
		}
		else
			hand.arrange()
	}
	
	@Suppress("UNCHECKED_CAST")
	fun onObjectDisown(event: ObjectDisownEvent)
	{
		val (id, x, y, rotation, isFaceUp, disownerUsername) = event
		val toDisown = idToGObjectMap[id] as Groupable<CardGroup>
		val hand = userToHandMap[disownerUsername]!!
		hand -= toDisown
		hand.arrange()
		toDisown.getModule<SmoothMovable>()?.apply {
			setPosition(x, y)
			this.rotation = rotation
		}
		toDisown.getModule<Lockable>()?.lock(users[disownerUsername]!!)
		if (toDisown is Card)
			toDisown.isFaceUp = isFaceUp
	}
	
	fun updateHand(ownerUsername: String)
	{
		(userToHandMap[ownerUsername] as? OpponentHand)?.flatten()
	}
	
	fun onObjectMove(event: ObjectMoveEvent)
	{
		val (id, x, y) = event
		val toMove = idToGObjectMap[id]!!
		toMove.getModule<SmoothMovable>()?.setPosition(x, y)
		if (toMove is CardGroup && toMove.type == ServerCardGroup.Type.PILE)
		{
			toMove.type = ServerCardGroup.Type.STACK
			toMove.arrange()
		}
		event.free()
	}
	
	fun onObjectRotate(event: ObjectRotateEvent)
	{
		val (id, rotation) = event
		idToGObjectMap[id]!!.getModule<SmoothMovable>()?.rotation = rotation
		event.free()
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
	
	fun onCardFlip(event: CardFlipEvent)
	{
		val card = idToGObjectMap[event.id] as Card
		card.isFaceUp = !card.isFaceUp
	}
	
	fun onCardGroupCreate(event: CardGroupCreateEvent)
	{
		val (id, serverCards) = event
		val cards = serverCards.map { idToGObjectMap[it.id] as Card }
		val firstX = cards.first().smoothMovable.x
		val firstY = cards.first().smoothMovable.y
		val firstRotation = cards.first().smoothMovable.rotation
		val cardGroup = CardGroup(room, id, firstX, firstY, firstRotation)
		this.cards.addActorAfter(cards.first(), cardGroup)
		cards.forEachIndexed { index, card: Card ->
			val (_, x, y, rotation) = serverCards[index]
			cardGroup += card
			card.smoothMovable.setPosition(x, y)
			card.smoothMovable.rotation = rotation
		}
		cardGroup.arrange()
		idToGObjectMap[id] = cardGroup
	}
	
	fun onCardGroupChange(event: CardGroupChangeEvent)
	{
		val (cards, newCardGroupId, changerUsername) = event
		if (changerUsername != game.user.name || newCardGroupId != -1)
		{
			val newCardGroup = if (newCardGroupId != -1) idToGObjectMap[newCardGroupId] as CardGroup else null
			for ((id, x, y, rotation, _, _, isFaceUp) in cards)
			{
				val card = idToGObjectMap[id] as Card
				val oldCardGroup = card.cardGroup
				card.cardGroup = newCardGroup
				card.smoothMovable.setPosition(x, y)
				card.smoothMovable.rotation = rotation
				card.isFaceUp = isFaceUp
				if (oldCardGroup != null)
					oldCardGroup.ownable.myHand?.arrange() ?: oldCardGroup.arrange()
			}
			newCardGroup?.arrange()
		}
	}
	
	fun onCardGroupDetach(event: CardGroupDetachEvent)
	{
		val (cardHolderId, replacementCardGroupId, changerUsername) = event
		val cardHolder = idToGObjectMap[cardHolderId] as CardHolder
		if (changerUsername != game.user.name)
		{
			val cardGroup = cardHolder.cardGroup!!
			cardGroup.transformToGroupCoordinates(cards)
			cards += cardGroup
		}
		val replacementCardGroup = CardGroup(room, replacementCardGroupId, type = cardHolder.defaultType)
		idToGObjectMap[replacementCardGroupId] = replacementCardGroup
		cardHolder += replacementCardGroup
	}
	
	fun onCardGroupDismantle(event: CardGroupDismantleEvent)
	{
		(idToGObjectMap[event.id] as CardGroup).dismantle()
	}
	
	fun onNewGame(event: NewGameEvent)
	{
		val (cardGroupChangeEvent, shuffleSeed, gameState) = event
		for (gObject: GObject<CrazyEights> in idToGObjectMap.values()) // Unlock and disown everything
		{
			gObject.getModule<Lockable>()?.unlock(false)
			gObject.getModule<Ownable>()?.wasInHand = false
		}
		onCardGroupChange(cardGroupChangeEvent!!)
		val drawStack = drawStack!!
		drawStack.flip(false)
		
		val userToHandMap = userToHandMap
		for (username: String in userToHandMap.orderedKeys().toArray(String::class.java)) // Remove hands of offline users
		{
			val hand = userToHandMap[username]
			if (username !in users)
			{
				userToHandMap.remove(username)
				hand!!.remove()
			}
			if (hand is OpponentHand)
				hand.isHandOpen = false
		}
		arrangePlayers()
		
		val hands: Array<Hand> = userToHandMap.orderedKeys().map { userToHandMap[it]!! }.toArray(Hand::class.java)
		
		drawStack += Actions.run {
			drawStackHolder.touchable = Touchable.disabled
			myHand.touchable = Touchable.disabled
		} then
			Actions.delay(1F, ShowCenterTitleAction(room, "Shuffling...")) then
			ShuffleAction(room, shuffleSeed) then
			Actions.delay(0.5F, ShowCenterTitleAction(room, "Dealing...")) then
			DealAction(room, hands) then
			HideCenterTitleAction(room) then
			Actions.run {
				drawStackHolder.touchable = Touchable.enabled
				myHand.touchable = Touchable.enabled
				game.client?.sendTCP(ActionLockReleaseEvent)
			}
		
		if (hands.size > 2)
		{
			playDirectionIndicator += Actions.fadeIn(2F)
			playDirectionIndicator.scaleX = 1F
		}
		
		this.gameState = gameState
	}
	
	fun onRulesetUpdate(event: RulesetUpdateEvent)
	{
		val (ruleset, changerUsername) = event
		if (changerUsername.isNotEmpty())
		{
			when
			{
				ruleset.maxDrawCount != this.ruleset.maxDrawCount ->
					room.chatBox.chat("$changerUsername set Max Draw Count to ${if (ruleset.maxDrawCount == Int.MAX_VALUE) 0 else ruleset.maxDrawCount}")
				ruleset.drawTwos != this.ruleset.drawTwos ->
					room.chatBox.chat("$changerUsername set Draw Twos to ${ruleset.drawTwos ?: "Off"}")
				ruleset.skips != this.ruleset.skips ->
					room.chatBox.chat("$changerUsername set Skips to ${ruleset.skips ?: "Off"}")
				ruleset.reverses != this.ruleset.reverses ->
					room.chatBox.chat("$changerUsername set Reverses to ${ruleset.reverses ?: "Off"}")
			}
		}
		this.ruleset = ruleset
		room.gameSettingsDialog.updateRuleset(ruleset)
	}
	
	fun onGameStateUpdated(gameState: GameState)
	{
		val (_, players, currentPlayer, isPlayReversed, drawCount, declaredSuit, drawTwoEffectCardCount, powerCardPlayedEvent) = gameState
		this.gameState = gameState
		
		if (currentPlayer == game.user.name)
		{
			passButton.isVisible = (drawCount >= gameState.ruleset.maxDrawCount || drawStack!!.cards.isEmpty)
				&& powerCardPlayedEvent !is EightsPlayedEvent
			drawStackHolder.touchable =
				if (drawCount >= gameState.ruleset.maxDrawCount
					|| powerCardPlayedEvent is EightsPlayedEvent && powerCardPlayedEvent.playerUsername == game.user.name)
					Touchable.disabled
				else
					Touchable.enabled
			drawStackHolder.isFlashing = drawTwoEffectCardCount > 0
			if (drawTwoEffectCardCount > 0)
				myHand.setDarkened { it.rank != gameState.ruleset.drawTwos }
			else
				myHand.setDarkened { powerCardPlayedEvent is EightsPlayedEvent && powerCardPlayedEvent.playerUsername == game.user.name }
		}
		else
		{
			passButton.isVisible = false
			drawStackHolder.touchable = Touchable.enabled
			drawStackHolder.isFlashing = false
			myHand.setDarkened { true }
		}
		
		if (players.size > 2 && MathUtils.isEqual(playDirectionIndicator.alpha, 0F))
		{
			playDirectionIndicator.scaleX = if (isPlayReversed) -1F else 1F
			playDirectionIndicator += Actions.fadeIn(2F)
		}
		
		if (declaredSuit != null && powerCardPlayedEvent !is EightsPlayedEvent)
		{
			if (suitChooser == null)
				onEightsPlayed(EightsPlayedEvent(""))
			if (suitChooser!!.chosenSuit == null)
				suitChooser!!.chosenSuit = declaredSuit
		}
		else
		{
			suitChooser = null
			when (powerCardPlayedEvent)
			{
				is EightsPlayedEvent -> onEightsPlayed(powerCardPlayedEvent)
				is DrawTwosPlayedEvent -> onDrawTwosPlayed(powerCardPlayedEvent)
				is ReversePlayedEvent -> onReversePlayed(powerCardPlayedEvent)
				is SkipsPlayedEvent -> onSkipsPlayed(powerCardPlayedEvent)
				else ->
				{
					for (actor: Actor in powerCardEffects.children)
					{
						actor.clearActions()
						actor += Actions.fadeOut(1F) then Actions.removeActor(actor)
					}
					
					if (drawTwoEffectCardCount > 0)
						powerCardEffects += EffectText(room, "+${drawTwoEffectCardCount}")
				}
			}
		}
	}
	
	fun onGameEnded(event: GameEndedEvent)
	{
		room.chatBox.chat("Game over! ${event.winner} won the game!", Color.YELLOW)
		passButton.isVisible = false
		drawStackHolder.touchable = Touchable.enabled
		drawStackHolder.isFlashing = false
		myHand.setDarkened { false }
		myHand.clearMemory()
		gameState = null
		for (hand: Hand in userToHandMap.values())
		{
			if (hand is OpponentHand && hand.user.isAi)
				hand.isHandOpen = true
		}
		playDirectionIndicator += Actions.fadeOut(2F)
		for (actor: Actor in powerCardEffects.children)
		{
			actor.clearActions()
			actor += Actions.fadeOut(1F) then Actions.removeActor(actor)
		}
	}
	
	fun onEightsPlayed(event: EightsPlayedEvent)
	{
		room.dramatic.play()
		val suitChooser = SuitChooser(room, event.playerUsername == game.user.name)
		this.suitChooser = suitChooser
		passButton.isVisible = false
		powerCardEffects.clearChildren()
		powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
			Actions.targeting(
				powerLabelGroup,
				Actions.fadeOut(0.5F)
			) along Actions.run { powerCardEffects += suitChooser }
		}
		persistentPowerCardEffects += PowerCardEffectRing(room)
	}
	
	fun onSuitsDeclared(event: SuitDeclareEvent)
	{
		suitChooser?.chosenSuit = event.suit
	}
	
	fun onDrawTwosPlayed(packet: DrawTwosPlayedEvent)
	{
		powerCardEffects.clearChildren()
		powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
			defaultAction along Actions.run {
				powerCardEffects += EffectText(room, "+${packet.drawCardCount}")
			}
		}
		persistentPowerCardEffects += PowerCardEffectRing(room)
	}
	
	fun onDrawTwoPenalty(event: DrawTwoPenaltyEvent)
	{
		val (victimUsername, drawCardCount) = event
		val hand = userToHandMap[victimUsername]!!
		val drawTwoEffectText = powerCardEffects.children.firstOrNull { it is EffectText } as? EffectText
		drawTwoEffectText?.moveToHand(hand)
		
		drawStack!! += Actions.run {
			drawStackHolder.isFlashing = false
			drawStackHolder.touchable = Touchable.disabled
			myHand.touchable = Touchable.disabled
		} then Actions.delay(1.5F, DrawAction(room, hand, drawCardCount)) then Actions.run {
			drawStackHolder.touchable = Touchable.enabled
			myHand.touchable = Touchable.enabled
			game.client?.sendTCP(ActionLockReleaseEvent)
		}
	}
	
	fun onSkipsPlayed(event: SkipsPlayedEvent)
	{
		powerCardEffects.clearChildren()
		powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
			defaultAction along Actions.run {
				powerCardEffects += EffectText(
					room, gameState!!.ruleset.skips?.toString() ?: "Q", userToHandMap[event.victimUsername]!!
				)
			}
		}
		persistentPowerCardEffects += PowerCardEffectRing(room)
	}
	
	fun onReversePlayed(event: ReversePlayedEvent)
	{
		powerCardEffects.clearChildren()
		powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
			defaultAction along Actions.run {
				persistentPowerCardEffects += PowerCardEffectRing(room)
				room.deepWhoosh.play()
				playDirectionIndicator.isReversed = event.isReversed
			}
		}
		persistentPowerCardEffects += PowerCardEffectRing(room)
	}
	
	fun onDrawStackRefill(event: DrawStackRefillEvent)
	{
		val (cardGroupChangeEvent, shuffleSeed) = event
		val drawStack = drawStack!!
		val discardPile = discardPile!!
		val discards = GdxArray(discardPile.cards)
		val topCard: Groupable<CardGroup> = discards.pop()
		
		for (discard: Groupable<CardGroup> in discards) // Unlock and disown everything in discards
		{
			discard.lockable.unlock(false)
			discard.getModule<Ownable>()?.wasInHand = false
		}
		
		onCardGroupChange(cardGroupChangeEvent!!)
		drawStack.flip(false)
		
		topCard.smoothMovable.apply {
			x = 0F
			y = 0F
			rotation = 180*round(rotation/180)
		}
		
		val prevDrawStackTouchable = drawStackHolder.touchable
		val prevMyHandTouchable = myHand.touchable
		drawStack += Actions.run {
			drawStackHolder.touchable = Touchable.disabled
			myHand.touchable = Touchable.disabled
			if (gameState?.currentPlayer == game.user.name)
				passButton.isVisible = discardPile.cards.size == 1 && drawStack.cards.isEmpty
		} then
			Actions.delay(0.5F, ShuffleAction(room, shuffleSeed)) then
			Actions.run {
				drawStackHolder.touchable = prevDrawStackTouchable
				myHand.touchable = prevMyHandTouchable
				game.client?.sendTCP(ActionLockReleaseEvent)
			}
	}
	
	fun onResetDeck(event: ResetDeckEvent)
	{
		val (cardGroupChangeEvent, shuffleSeed) = event
		for (gObject: GObject<CrazyEights> in idToGObjectMap.values()) // Unlock and disown everything
		{
			gObject.getModule<Lockable>()?.unlock(false)
			gObject.getModule<Ownable>()?.wasInHand = false
		}
		onCardGroupChange(cardGroupChangeEvent!!)
		val drawStack = drawStack!!
		drawStack.flip(false)
		drawStack += Actions.run {
			drawStackHolder.touchable = Touchable.disabled
			myHand.touchable = Touchable.disabled
		} then
			Actions.delay(0.5F, ShuffleAction(room, shuffleSeed)) then
			Actions.run {
				drawStackHolder.touchable = Touchable.enabled
				myHand.touchable = Touchable.enabled
				game.client?.sendTCP(ActionLockReleaseEvent)
			}
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
		passButton.isVisible = false
		
		gameState = null
		suitChooser = null
	}
}
