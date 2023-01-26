package misterbander.crazyeights.net.server

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.Queue
import com.esotericsoftware.kryonet.Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.SERVER_UPDATES_PER_SECOND
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.CardSlideSoundEvent
import misterbander.crazyeights.net.packets.Chat
import misterbander.crazyeights.net.packets.DrawStackRefillEvent
import misterbander.crazyeights.net.packets.DrawTwoPenaltyEvent
import misterbander.crazyeights.net.packets.DrawTwosPlayedEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.GameEndedEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.PowerCardPlayedEvent
import misterbander.crazyeights.net.packets.ResetDeckEvent
import misterbander.crazyeights.net.packets.ReversePlayedEvent
import misterbander.crazyeights.net.packets.RulesetUpdateEvent
import misterbander.crazyeights.net.packets.SkipsPlayedEvent
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.net.server.ServerCard.Rank
import misterbander.crazyeights.net.server.ServerCard.Suit
import misterbander.crazyeights.net.server.game.ChangeSuitMove
import misterbander.crazyeights.net.server.game.DrawMove
import misterbander.crazyeights.net.server.game.DrawTwoEffectPenalty
import misterbander.crazyeights.net.server.game.PassMove
import misterbander.crazyeights.net.server.game.PlayMove
import misterbander.crazyeights.net.server.game.Player
import misterbander.crazyeights.net.server.game.Ruleset
import misterbander.crazyeights.net.server.game.ServerGameState
import misterbander.crazyeights.net.server.game.ai.IsmctsAgent
import kotlin.math.min
import kotlin.math.round

class ServerTabletop(
	val parent: CrazyEightsServer,
	hands: Map<User, GdxArray<ServerObject>> = emptyMap(),
	val drawStackHolder: ServerCardHolder,
	val discardPileHolder: ServerCardHolder
)
{
	val idToObjectMap = IntMap<ServerObject>()
	
	val users = GdxMap<String, User>()
	val userCount: Int
		get() = users.size
	val hands = OrderedMap<String, GdxArray<ServerObject>>()
	private val aiNames = gdxArrayOf("Shark (AI)", "Queenpin (AI)", "Watson (AI)", "Ning (AI)")
	private var aiCount = 0
	val aiJobs = Queue<Job>()
	private val actionLocks = GdxSet<String>()
	
	val serverObjects = GdxArray<ServerObject>()
	
	private var ruleset = Ruleset(firstDiscardOnDealTriggersPower = true)
	var serverGameState: ServerGameState? = null
	private val isGameStarted: Boolean
		get() = serverGameState != null
	private var lastPowerCardPlayedEvent: PowerCardPlayedEvent? = null
	private var suitChooser: String? = null
	
	@Volatile var serverObjectsDebugString: String = ""
		private set
	@Volatile var handsDebugString: String = ""
		private set
	
	init
	{
		for ((user, hand) in hands)
		{
			users[user.name] = user
			this.hands[user.name] = hand.onEach { idToObjectMap[it.id] = it }
		}
		addServerObject(drawStackHolder)
		addServerObject(discardPileHolder)
	}
	
	inline fun <reified T : ServerObject> findObjectById(id: Int): T = idToObjectMap[id] as? T
		?: throw IllegalArgumentException("Expected ${T::class.java} with id = $id, found ${idToObjectMap[id]}")
	
	fun addServerObject(serverObject: ServerObject, insertAtIndex: Int = -1)
	{
		idToObjectMap[serverObject.id] = serverObject
		if (serverObject is ServerCardGroup)
			serverObject.cards.forEach { idToObjectMap[it.id] = it }
		else if (serverObject is ServerCardHolder)
		{
			idToObjectMap[serverObject.cardGroup.id] = serverObject.cardGroup
			serverObject.cardGroup.cards.forEach { idToObjectMap[it.id] = it }
		}
		if (insertAtIndex != -1)
			serverObjects.insert(insertAtIndex, serverObject)
		else
			serverObjects += serverObject
	}
	
	private fun toTabletopState(): TabletopState = TabletopState(users, serverObjects, hands)
	
	fun updateDebugStrings()
	{
		val serverObjectsStr =
			serverObjects.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }
		serverObjectsDebugString = "Server objects (${serverObjects.size}):\n$serverObjectsStr"
		val handsStr = hands.joinToString(separator = "\n") { (key, value) ->
			"$key: ${
				value?.joinToString(
					separator = ",\n",
					prefix = "[\n",
					postfix = "\n]"
				) { "    $it" }
			}"
		}
		handsDebugString = "Hands:\n$handsStr"
	}
	
	fun addUser(user: User)
	{
		users[user.name] = user
		hands.getOrPut(user.name) { GdxArray() }
		parent.server.sendToAllTCP(UserJoinedEvent(user))
		if (!user.isAi)
			info("Server | INFO") { "${user.name} joined the game" }
	}
	
	fun removeUser(user: User)
	{
		users.remove(user.name) ?: throw IllegalArgumentException("Can't find user ${user.name}")
		val hand = hands[user.name] ?: throw IllegalStateException("Can't find hand for ${user.name}")
		if (user.isAi)
		{
			hands.remove(user.name)
			for (card: ServerObject in hand)
				(card as ServerCard).setServerCardGroup(this, null)
			parent.server.sendToAllTCP(UserLeftEvent(user))
			return
		}
		actionLocks -= user.name
		if (hand.isEmpty)
			hands.remove(user.name)
		for (serverObject: ServerObject in idToObjectMap.values())
		{
			if (serverObject is ServerLockable && serverObject.lockHolder == user.name)
			{
				serverObject.lockHolder = null
				if (serverObject is ServerCard)
				{
					serverObject.justMoved = false
					serverObject.justRotated = false
					if (isGameStarted && serverObject.lastOwner == user.name)
					{
						serverObject.isFaceUp = true
						serverObject.lockHolder = null
						serverObject.setOwner(this, user.name)
						parent.server.sendToAllTCP(ObjectOwnEvent(serverObject.id, user.name))
					}
				}
			}
		}
		parent.server.sendToAllTCP(UserLeftEvent(user))
		info("Server | INFO") { "${user.name} left the game" }
	}
	
	fun onUserJoined(connection: Connection, user: User)
	{
		addUser(user)
		connection.arbitraryData = user
		connection.sendTCP(toTabletopState())
		connection.sendTCP(RulesetUpdateEvent(ruleset))
		if (isGameStarted)
			connection.sendTCP(
				serverGameState!!.toGameState(
					if (suitChooser != null) EightsPlayedEvent(suitChooser!!) else null
				)
			)
	}
	
	fun onSwapSeats(event: SwapSeatsEvent)
	{
		val (user1, user2) = event
		val keys: GdxArray<String> = hands.orderedKeys()
		val index1 = keys.indexOf(user1, false)
		val index2 = keys.indexOf(user2, false)
		if (index1 == -1)
			throw IllegalArgumentException("Can't find user $user1")
		if (index2 == -1)
			throw IllegalArgumentException("Can't find user $user2")
		keys.swap(index1, index2)
		parent.server.sendToAllTCP(event)
		info("Server | INFO") { "$user1 swapped seats with $user2" }
	}
	
	fun onAiAdd()
	{
		if (aiCount >= 6)
			return
		aiCount++
		val name = aiNames.random() ?: "AI $aiCount"
		aiNames -= name
		addUser(User(name, Color.LIGHT_GRAY, true))
	}
	
	fun onAiRemove(event: AiRemoveEvent)
	{
		aiCount--
		if (!event.username.startsWith("AI "))
			aiNames += event.username
		removeUser(users[event.username] ?: throw IllegalArgumentException("Can't find user ${event.username}"))
	}
	
	fun onObjectLock(event: ObjectLockEvent)
	{
		if (actionLocks.isNotEmpty())
			return
		val (id, lockerUsername) = event
		val toLock = findObjectById<ServerLockable>(id)
		if (!toLock.canLock) // Only unlocked draggables can be locked
			return
		if (isGameStarted)
		{
			val serverGameState = serverGameState!!
			if (lockerUsername != serverGameState.currentPlayer.name) // You can't lock anything if it's not your turn
				return
			if (toLock is ServerCard)
			{
				if (toLock.cardGroupId == -1) // Stray cards cannot be locked
					return
				val cardGroup = findObjectById<ServerCardGroup>(toLock.cardGroupId)
				if (cardGroup.cardHolderId == drawStackHolder.id)
				{
					if (serverGameState.drawCount >= serverGameState.ruleset.maxDrawCount)
						return
					if (serverGameState.drawTwoEffectCardCount > 0)
					{
						acceptDrawTwoPenalty(lockerUsername)
						return
					}
					serverGameState.doMove(DrawMove)
				}
				else if (cardGroup.cardHolderId == discardPileHolder.id) // Discard pile cannot be locked
					return
			}
			else if (toLock is ServerCardGroup) // Card groups can't be locked during games
				return
		}
		debug("Server | DEBUG") { "$lockerUsername locks $toLock" }
		toLock.lockHolder = lockerUsername
		toLock.toFront(this)
		parent.server.sendToAllTCP(event)
	}
	
	fun onObjectUnlock(event: ObjectUnlockEvent)
	{
		val (id, unlockerUsername, sideEffects) = event
		val toUnlock = findObjectById<ServerLockable>(id)
		if (toUnlock.lockHolder != unlockerUsername)
			return
		debug("Server | DEBUG") { "${toUnlock.lockHolder} unlocks $toUnlock" }
		toUnlock.lockHolder = null
		if (toUnlock is ServerCard)
		{
			if (toUnlock.cardGroupId != -1)
			{
				val cardGroup = findObjectById<ServerCardGroup>(toUnlock.cardGroupId)
				if (isGameStarted && cardGroup.cardHolderId == drawStackHolder.id)
				{
					parent.server.sendToAllTCP(event)
					draw(cardGroup.cards.peek(), unlockerUsername, fireOwnEvent = true, playSound = true)
					return
				}
				else
					cardGroup.arrange()
			}
			else if (isGameStarted)
			{
				// If the card is left on the table without being in someone's hand or in a card group, then it will be
				// returned to its original owner
				KtxAsync.launch {
					delay(1000/SERVER_UPDATES_PER_SECOND.toLong())
					if (toUnlock.cardGroupId == -1 && toUnlock.getOwner(this@ServerTabletop) == null)
						draw(toUnlock, toUnlock.lastOwner ?: serverGameState!!.currentPlayer.name, fireOwnEvent = true)
				}
			}
			if (!toUnlock.justMoved && !toUnlock.justRotated && sideEffects && !isGameStarted)
			{
				toUnlock.isFaceUp = !toUnlock.isFaceUp
				parent.server.sendToAllTCP(CardFlipEvent(id))
			}
			toUnlock.justMoved = false
			toUnlock.justRotated = false
		}
		else if (toUnlock is ServerCardGroup)
		{
			if (toUnlock.cardHolderId != -1)
				toUnlock.rotation = 0F
		}
		parent.server.sendToAllTCP(event)
	}
	
	fun onObjectOwn(connection: Connection, event: ObjectOwnEvent)
	{
		val (id, ownerUsername) = event
		val toOwn = findObjectById<ServerOwnable>(id)
		if (toOwn.getOwner(this) != null)
			return
		toOwn.setOwner(this, ownerUsername)
		parent.server.sendToAllExceptTCP(connection.id, event)
	}
	
	fun onObjectDisown(connection: Connection, event: ObjectDisownEvent)
	{
		val (id, x, y, rotation, isFaceUp, disownerUsername) = event
		val toDisown = findObjectById<ServerOwnable>(id)
		if (toDisown.getOwner(this) != disownerUsername)
			return
		toDisown.setOwner(this, null)
		findObjectById<ServerOwnable>(id).apply {
			this.x = x
			this.y = y
			this.rotation = rotation
			lockHolder = disownerUsername
			if (this is ServerCard)
				this.isFaceUp = isFaceUp
		}
		parent.server.sendToAllExceptTCP(connection.id, event)
	}
	
	fun onHandUpdate(connection: Connection, event: HandUpdateEvent)
	{
		val (hand, ownerUsername) = event
		hands[ownerUsername] = hand
		hand.forEach { idToObjectMap[it.id] = it }
		parent.server.sendToAllExceptTCP(connection.id, event)
	}
	
	fun onObjectMove(connection: Connection, event: ObjectMoveEvent)
	{
		if (actionLocks.isNotEmpty())
			return
		val (id, x, y) = event
		val toMove = findObjectById<ServerLockable>(id)
		if (toMove.lockHolder?.let { users[it] } != connection.arbitraryData)
			return
		toMove.x = x
		toMove.y = y
		if (toMove is ServerCard)
			toMove.justMoved = true
		else if (toMove is ServerCardGroup)
		{
			if (toMove.isLocked && toMove.type == ServerCardGroup.Type.PILE)
				toMove.type = ServerCardGroup.Type.STACK
		}
		parent.server.sendToAllExceptTCP(connection.id, event)
		event.free()
	}
	
	fun onObjectRotate(connection: Connection, event: ObjectRotateEvent)
	{
		if (actionLocks.isNotEmpty())
			return
		val (id, rotation) = event
		val toRotate = findObjectById<ServerLockable>(id)
		if (toRotate.lockHolder?.let { users[it] } != connection.arbitraryData)
			return
		if (toRotate is ServerCard)
			toRotate.justRotated = true
		toRotate.rotation = rotation
		parent.server.sendToAllExceptTCP(connection.id, event)
		event.free()
	}
	
	fun onCardGroupCreate(event: CardGroupCreateEvent)
	{
		if (isGameStarted)
			return
		val cards = event.cards
		val (firstId, firstX, firstY, firstRotation) = cards[0]
		val cardGroup = ServerCardGroup(parent.newId(), firstX, firstY, firstRotation)
		val insertAtIndex = serverObjects.indexOfFirst { it.id == firstId }
		cards.forEachIndexed { index, (id, x, y, rotation) ->
			val card = findObjectById<ServerCard>(id)
			serverObjects.removeValue(card, true)
			card.x = x
			card.y = y
			card.rotation = rotation
			cards[index] = card
			cardGroup.addCard(this, card)
		}
		cardGroup.arrange()
		addServerObject(cardGroup, insertAtIndex)
		parent.server.sendToAllTCP(event.copy(id = cardGroup.id))
	}
	
	fun onCardGroupChange(event: CardGroupChangeEvent)
	{
		val (cards, newCardGroupId) = event
		val newCardGroup = if (newCardGroupId != -1) findObjectById<ServerCardGroup>(newCardGroupId) else null
		
		if (isGameStarted && newCardGroup?.cardHolderId == discardPileHolder.id) // User discards a card
		{
			play(event)
			return
		}
		
		cards.forEachIndexed { index, (id, _, _, rotation) ->
			val card = findObjectById<ServerCard>(id)
			card.rotation = rotation
			card.setServerCardGroup(this, newCardGroup)
			cards[index] = card
		}
		newCardGroup?.arrange()
		parent.server.sendToAllTCP(event)
	}
	
	fun onCardGroupDetach(event: CardGroupDetachEvent)
	{
		if (isGameStarted)
			return
		val (cardHolderId, _, changerUsername) = event
		val cardHolder = findObjectById<ServerCardHolder>(cardHolderId)
		val cardGroup = cardHolder.cardGroup
		cardGroup.apply {
			x = cardHolder.x
			y = cardHolder.y
			rotation = cardHolder.rotation
			this.cardHolderId = -1
		}
		serverObjects += cardGroup
		val replacementCardGroup = ServerCardGroup(parent.newId(), type = cardHolder.defaultType)
		idToObjectMap[replacementCardGroup.id] = replacementCardGroup
		cardHolder.cardGroup = replacementCardGroup
		replacementCardGroup.cardHolderId = cardHolder.id
		parent.server.sendToAllTCP(CardGroupDetachEvent(cardHolderId, cardHolder.cardGroup.id, changerUsername))
	}
	
	fun onCardGroupDismantle(connection: Connection, event: CardGroupDismantleEvent)
	{
		if (isGameStarted)
			return
		val cardGroup = findObjectById<ServerCardGroup>(event.id)
		while (cardGroup.cards.isNotEmpty())
		{
			val card: ServerCard = cardGroup.cards.removeIndex(0)
			card.setServerCardGroup(this, null)
		}
		idToObjectMap.remove(cardGroup.id)
		serverObjects.removeValue(cardGroup, true)
		parent.server.sendToAllExceptTCP(connection.id, event)
	}
	
	fun onNewGame(connection: Connection)
	{
		if (actionLocks.isNotEmpty())
			return
		val drawStack = drawStackHolder.cardGroup
		val discardPile = discardPileHolder.cardGroup
		val seed = MathUtils.random.nextLong()
//		val seed = 9020568252116114615 // Starting hand with 8, A
//		val seed = -5000073366615045381 // Starting hand with 2
//		val seed = 2212245332158196130 // Starting hand with Q
//		val seed = -3202561125370556140 // Starting hand with A
//		val seed = 1505641440241536783 // First discard is 8
//		val seed = 1997011525088092652 // First discard is Q
		val cardGroupChangeEvent = resetDeck(seed, true)
		
		// Deal
		repeat(if (hands.size > 2) 5 else 7) {
			for (username: String in hands.orderedKeys())
				draw(drawStack.cards.peek(), username, refillIfEmpty = false)
		}
		val topCard: ServerCard = drawStack.cards.peek()
		topCard.setServerCardGroup(this, discardPile)
		topCard.isFaceUp = true
		acquireActionLocks()
		
		// Set game state
		val serverGameState = createGameState(::onPlayerChanged)
		this.serverGameState = serverGameState
		val firstPlayer = serverGameState.currentPlayer.name
		
		parent.server.sendToAllTCP(
			Chat(
				message = "${(connection.arbitraryData as User).name} started a new game",
				isSystemMessage = true
			)
		)
		parent.server.sendToAllTCP(NewGameEvent(cardGroupChangeEvent, seed, serverGameState.toGameState()))
		
		KtxAsync.launch {
			waitForActionLocks()
			val firstPower = serverGameState.triggerFirstPowerCard()
			when
			{
				ruleset.drawTwos != null && firstPower == ruleset.drawTwos ->
				{
					lastPowerCardPlayedEvent = DrawTwosPlayedEvent(serverGameState.drawTwoEffectCardCount)
					parent.server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
				}
				ruleset.skips != null && firstPower == ruleset.skips ->
				{
					lastPowerCardPlayedEvent = SkipsPlayedEvent(firstPlayer)
					parent.server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
				}
				ruleset.reverses != null && firstPower == ruleset.reverses && serverGameState.playerCount > 2 ->
				{
					lastPowerCardPlayedEvent = ReversePlayedEvent(serverGameState.isPlayReversed)
					parent.server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
				}
			}
			if (users[serverGameState.currentPlayer.name].isAi)
				onPlayerChanged(serverGameState.currentPlayer)
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	fun createGameState(onPlayerChanged: (Player) -> Unit = {}): ServerGameState
	{
		val playerHands = OrderedMap<Player, GdxArray<ServerCard>>()
		val drawStack = drawStackHolder.cardGroup
		val discardPile = discardPileHolder.cardGroup
		for ((username, hand) in hands)
		{
			val user = users[username] ?: throw IllegalStateException("Can't find user $username")
			if (user.isAi)
				playerHands[IsmctsAgent(username)] = GdxArray(hand) as GdxArray<ServerCard>
			else
				playerHands[user] = GdxArray(hand) as GdxArray<ServerCard>
		}
		return ServerGameState(
			ruleset,
			playerHands,
			GdxArray(drawStack.cards),
			GdxArray(discardPile.cards),
			onPlayerChanged = onPlayerChanged
		)
	}
	
	/**
	 * Some actions play a client-side animation which takes some time. While the animation is playing, we must ensure
	 * that no other events take place to prevent events from overlapping, causing strange behavior. This is achieved
	 * using action locks.
	 * If an event that plays a client-side animation occurs, each currently online user will obtain an action lock.
	 * Action locks will only be released once the client-side animation finishes, or the user leaves the room.
	 */
	private fun acquireActionLocks()
	{
		for ((username, user) in users)
		{
			if (!user!!.isAi)
				actionLocks += username
		}
		debug("Server | DEBUG") { "Acquired action locks: $actionLocks" }
	}
	
	fun onActionLockReleaseEvent(connection: Connection)
	{
		actionLocks -= (connection.arbitraryData as User).name
	}
	
	private suspend fun waitForActionLocks()
	{
		debug("Server | DEBUG") { "Waiting for action locks: remaining = $actionLocks" }
		while (true)
		{
			if (actionLocks.isEmpty)
				break
			delay(1000/SERVER_UPDATES_PER_SECOND.toLong())
		}
	}
	
	fun onRulesetUpdate(event: RulesetUpdateEvent)
	{
		if (isGameStarted)
			return
		ruleset = event.ruleset
		parent.server.sendToAllTCP(event)
	}
	
	fun play(cardGroupChangeEvent: CardGroupChangeEvent)
	{
		val (cards, newCardGroupId, playerUsername) = cardGroupChangeEvent
		assert(cards.size == 1) { "Playing more than 1 card: $cards" }
		val discardPile = findObjectById<ServerCardGroup>(newCardGroupId)
		val card = findObjectById<ServerCard>(cards.first().id)
		val playerHand = hands[playerUsername] ?: throw IllegalStateException("Can't find hand for user $playerUsername")
		val serverGameState = serverGameState!!
		val extraPackets = GdxArray<Any>()
		
		// Ignore if it's not the user's turn, or if the suit chooser is active and suit has not been declared yet,
		// or if not all action locks have been released
		if (playerUsername != serverGameState.currentPlayer.name
			|| serverGameState.declaredSuit == null && suitChooser != null
			|| actionLocks.isNotEmpty())
			return
		val move =
			if (card.rank == Rank.EIGHT) ChangeSuitMove(card, Suit.DIAMONDS) else PlayMove(card)
		if (move !in serverGameState.moves)
			return
		when
		{
			card.rank == Rank.EIGHT ->
			{
				suitChooser = playerUsername
				lastPowerCardPlayedEvent = EightsPlayedEvent(playerUsername)
				extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
			}
			card.rank == serverGameState.ruleset.drawTwos ->
			{
				serverGameState.doMove(move)
				lastPowerCardPlayedEvent = DrawTwosPlayedEvent(serverGameState.drawTwoEffectCardCount)
				extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
			}
			card.rank == serverGameState.ruleset.skips ->
			{
				lastPowerCardPlayedEvent = SkipsPlayedEvent(serverGameState.nextPlayer.name)
				serverGameState.doMove(move)
				extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
			}
			card.rank == serverGameState.ruleset.reverses && serverGameState.playerCount > 2 ->
			{
				serverGameState.doMove(move)
				lastPowerCardPlayedEvent = ReversePlayedEvent(serverGameState.isPlayReversed)
				extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
			}
			else ->
			{
				serverGameState.doMove(move)
				lastPowerCardPlayedEvent = null
				extraPackets += serverGameState.toGameState()
			}
		}
		if (card.rank != Rank.EIGHT)
			suitChooser = null
		if (playerHand.removeValue(card, true))
			extraPackets += CardSlideSoundEvent
		card.rotation = cards[0].rotation
		card.isFaceUp = true
		card.setServerCardGroup(this, discardPile)
		cards[0] = card
		discardPile.arrange()
		parent.server.sendToAllTCP(cardGroupChangeEvent)
		
		if (playerHand.isEmpty) // Winner!
		{
			this.serverGameState = null
			aiJobs.forEach { it.cancel() }
			aiJobs.clear()
			if (CardSlideSoundEvent in extraPackets)
				parent.server.sendToAllTCP(CardSlideSoundEvent)
			parent.server.sendToAllTCP(GameEndedEvent(playerUsername))
			suitChooser = null
			lastPowerCardPlayedEvent = null
			return
		}
		
		extraPackets.forEach { parent.server.sendToAllTCP(it) }
		
		val drawStack = drawStackHolder.cardGroup
		if (drawStack.cards.isEmpty && card.rank != Rank.EIGHT)
			refillDrawStack()
	}
	
	private fun onPlayerChanged(player: Player)
	{
		if (!isGameStarted)
			return
		val serverGameState = serverGameState!!
		val drawStack = drawStackHolder.cardGroup
		val discardPile = discardPileHolder.cardGroup
		if (users[player.name]?.isAi != true)
			return
		
		val firstAiJob = if (!aiJobs.isEmpty) aiJobs.first() else null
		aiJobs.addFirst(KtxAsync.launch {
			firstAiJob?.join()
			var justDrew = false
			do
			{
				val moveDeferred = async(parent.asyncContext) { (player as IsmctsAgent).getMove(serverGameState) }
				delay(if (justDrew) 800 else lastPowerCardPlayedEvent?.delayMillis ?: 1000)
				waitForActionLocks()
				justDrew = false
				lastPowerCardPlayedEvent = null
				val move = moveDeferred.await()
				info("Server | DEBUG") { "${player.name} best move = $move" }
				when (move)
				{
					is PlayMove ->
					{
						val card = findObjectById<ServerCard>(move.card.id)
						play(CardGroupChangeEvent(gdxArrayOf(card), discardPile.id, player.name))
					}
					is ChangeSuitMove ->
					{
						val card = findObjectById<ServerCard>(move.card.id)
						play(CardGroupChangeEvent(gdxArrayOf(card), discardPile.id, player.name))
						delay(3000)
						onSuitDeclare(event = SuitDeclareEvent(move.declaredSuit))
					}
					is DrawMove ->
					{
						val drawnCard: ServerCard = drawStack.cards.peek()
						serverGameState.doMove(move)
						draw(drawnCard, player.name, fireOwnEvent = true, playSound = true)
						justDrew = true
					}
					is DrawTwoEffectPenalty -> acceptDrawTwoPenalty(player.name)
					is PassMove -> pass()
				}
			}
			while (justDrew)
			if (!isActive)
				throw CancellationException()
			aiJobs.removeLast()
		})
	}
	
	fun draw(
		card: ServerCard,
		ownerUsername: String,
		fireOwnEvent: Boolean = false,
		playSound: Boolean = false,
		refillIfEmpty: Boolean = true
	)
	{
		card.isFaceUp = true
		card.setOwner(this, ownerUsername)
		if (fireOwnEvent)
			parent.server.sendToAllTCP(ObjectOwnEvent(card.id, ownerUsername))
		if (playSound)
			parent.server.sendToAllTCP(CardSlideSoundEvent)
		
		val drawStack = drawStackHolder.cardGroup
		if (drawStack.cards.isEmpty && refillIfEmpty)
			refillDrawStack()
	}
	
	fun pass()
	{
		val serverGameState = serverGameState!!
		serverGameState.doMove(PassMove)
		parent.server.sendToAllTCP(serverGameState.toGameState())
		lastPowerCardPlayedEvent = null
	}
	
	private fun refillDrawStack()
	{
		val drawStack = drawStackHolder.cardGroup
		val discardPileHolder = discardPileHolder
		val discardPile = discardPileHolder.cardGroup
		val serverGameState = serverGameState!!
		
		// Recall all discards
		val discards = GdxArray(discardPile.cards)
		val topCard: ServerCard = discards.pop() // Except the top card
		for (discard: ServerObject in discards) // Unlock everything and move all cards to the draw stack
		{
			if (discard is ServerLockable)
				discard.lockHolder = null
			if (discard is ServerCard && discard.cardGroupId != drawStack.id)
			{
				discard.setServerCardGroup(this, drawStack)
				discard.isFaceUp = false
				discard.lastOwner = null
			}
		}
		
		val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
		
		// Shuffle draw stack
		val seed = MathUtils.random.nextLong()
		debug("Server | DEBUG") { "Shuffling with seed = $seed" }
		drawStack.shuffle(this, seed)
		
		// Rearrange the top card nicely
		topCard.apply {
			x = 0F
			y = 0F
			rotation = 180*round(rotation/180)
		}
		
		// Set game state and action lock
		acquireActionLocks()
		serverGameState.drawStack.clear()
		serverGameState.drawStack += drawStack.cards
		serverGameState.discardPile.clear()
		serverGameState.discardPile += topCard
		serverGameState.currentPlayerHand.clear()
		hands[serverGameState.currentPlayer.name]!!.forEach { serverGameState.currentPlayerHand += it as ServerCard }
		
		parent.server.sendToAllTCP(DrawStackRefillEvent(cardGroupChangeEvent, seed))
	}
	
	fun onSuitDeclare(connection: Connection? = null, event: SuitDeclareEvent)
	{
		info("Server | INFO") { "Suit changed to ${event.suit.name}" }
		KtxAsync.launch {
			val serverGameState = serverGameState!!
			val topCard: ServerCard = discardPileHolder.cardGroup.cards.peek()
			delay(1000)
			serverGameState.doMove(ChangeSuitMove(topCard, event.suit))
			val drawStack = drawStackHolder.cardGroup
			if (drawStack.cards.isEmpty)
				refillDrawStack()
			parent.server.sendToAllTCP(serverGameState.toGameState())
		}
		if (connection == null)
			parent.server.sendToAllTCP(event)
		else
			parent.server.sendToAllExceptTCP(connection.id, event)
	}
	
	private fun acceptDrawTwoPenalty(acceptorUsername: String)
	{
		val serverGameState = serverGameState!!
		val drawStack = drawStackHolder.cardGroup
		
		KtxAsync.launch {
			if (drawStack.cards.size < serverGameState.drawTwoEffectCardCount)
				refillDrawStack()
			waitForActionLocks()
			
			acquireActionLocks()
			val drawCount = min(drawStack.cards.size, serverGameState.drawTwoEffectCardCount)
			repeat(drawCount) { draw(drawStack.cards.peek(), acceptorUsername, refillIfEmpty = false) }
			parent.server.sendToAllTCP(DrawTwoPenaltyEvent(acceptorUsername, drawCount))
			lastPowerCardPlayedEvent = null
			
			waitForActionLocks()
			serverGameState.doMove(DrawTwoEffectPenalty(serverGameState.drawTwoEffectCardCount))
			parent.server.sendToAllTCP(serverGameState.toGameState())
		}
	}
	
	fun onResetDeck()
	{
		if (actionLocks.isNotEmpty())
			return
		val seed = MathUtils.random.nextLong()
		val cardGroupChangeEvent = resetDeck(seed, false)
		acquireActionLocks()
		parent.server.sendToAllTCP(ResetDeckEvent(cardGroupChangeEvent, seed))
	}
	
	private fun resetDeck(seed: Long, removeOffline: Boolean): CardGroupChangeEvent
	{
		val drawStack = drawStackHolder.cardGroup
		
		// Recall all cards
		val serverObjects = idToObjectMap.values().toArray()
		for (serverObject: ServerObject in serverObjects) // Unlock everything and move all cards to the draw stack
		{
			if (serverObject is ServerLockable)
				serverObject.lockHolder = null
			if (serverObject is ServerCard)
			{
				if (serverObject.cardGroupId != drawStack.id)
					serverObject.setServerCardGroup(this, drawStack)
				serverObject.isFaceUp = false
				serverObject.lastOwner = null
			}
		}
		for (serverObject: ServerObject in serverObjects) // Remove all empty card groups
		{
			if (serverObject is ServerCardGroup && serverObject.cardHolderId == -1)
			{
				idToObjectMap.remove(serverObject.id)
				serverObjects.removeValue(serverObject, true)
			}
		}
		if (removeOffline)
		{
			for (username in hands.orderedKeys().toArray(String::class.java)) // Remove hands of offline users
			{
				if (username !in users)
					hands.remove(username)
			}
		}
		hands.values().forEach { it.clear() }
		
		val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
		
		// Shuffle draw stack
		debug("Server | DEBUG") { "Shuffling with seed = $seed" }
		drawStack.shuffle(this, seed)
		
		return cardGroupChangeEvent
	}
}
