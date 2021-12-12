package misterbander.crazyeights.net.packets

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryonet.Connection
import kotlinx.coroutines.launch
import ktx.actors.along
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.KtxAsync
import ktx.async.skipFrame
import ktx.collections.*
import ktx.log.debug
import ktx.log.info
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.PassMove
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.Ruleset
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.ai.IsmctsAgent
import misterbander.crazyeights.game.draw
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.GameState
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.EffectText
import misterbander.crazyeights.scene2d.Hand
import misterbander.crazyeights.scene2d.PowerCardEffect
import misterbander.crazyeights.scene2d.PowerCardEffectRing
import misterbander.crazyeights.scene2d.SuitChooser
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.actions.DealAction
import misterbander.crazyeights.scene2d.actions.DrawAction
import misterbander.crazyeights.scene2d.actions.HideCenterTitleAction
import misterbander.crazyeights.scene2d.actions.ShowCenterTitleAction
import misterbander.crazyeights.scene2d.actions.ShuffleAction
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.gframework.scene2d.GObject

data class NewGameEvent(
	val cardGroupChangeEvent: CardGroupChangeEvent? = null,
	val shuffleSeed: Long = 0,
	val gameState: GameState? = null
)

fun Tabletop.onNewGame(event: NewGameEvent)
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
	for ((username, hand) in userToHandMap.toGdxArray()) // Remove hands of offline users
	{
		if (username !in users)
		{
			userToHandMap.remove(username)
			hand!!.remove()
		}
	}
	arrangePlayers()
	
	val hands: Array<Hand> = userToHandMap.orderedKeys().map { userToHandMap[it]!! }.toArray(Hand::class.java)
	
	drawStack += Actions.run {
		drawStackHolder!!.touchable = Touchable.disabled
		myHand.touchable = Touchable.disabled
	} then
		delay(1F, ShowCenterTitleAction(room, "Shuffling...")) then
		ShuffleAction(room, shuffleSeed) then
		delay(0.5F, ShowCenterTitleAction(room, "Dealing...")) then
		DealAction(room, hands) then
		HideCenterTitleAction(room) then
		Actions.run {
			drawStackHolder!!.touchable = Touchable.enabled
			myHand.touchable = Touchable.enabled
			game.client?.sendTCP(ActionLockReleaseEvent)
		}
	
	if (hands.size > 2)
		playDirectionIndicator += fadeIn(2F)
	
	room.gameState = gameState
}

@Suppress("UNCHECKED_CAST")
fun CrazyEightsServer.onNewGame()
{
	val idToObjectMap = tabletop.idToObjectMap
	val drawStack = (idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	val discardPile = (idToObjectMap[tabletop.discardPileHolderId] as ServerCardHolder).cardGroup
	
	// Recall all cards
	val serverObjects = idToObjectMap.values().toArray()
	for (serverObject: ServerObject in serverObjects) // Unlock everything and move all cards to the draw stack
	{
		if (serverObject is ServerLockable)
			serverObject.lockHolder = null
		if (serverObject is ServerCard && serverObject.cardGroupId != drawStack.id)
		{
			serverObject.setServerCardGroup(drawStack, tabletop)
			serverObject.isFaceUp = false
		}
	}
	for (serverObject: ServerObject in serverObjects) // Remove all empty card groups
	{
		if (serverObject is ServerCardGroup && serverObject.cardHolderId == -1)
		{
			idToObjectMap.remove(serverObject.id)
			tabletop.serverObjects.removeValue(serverObject, true)
		}
	}
	for (username in tabletop.hands.orderedKeys().toArray(String::class.java)) // Remove hands of offline users
	{
		if (username !in tabletop.users)
			tabletop.hands.remove(username)
	}
	tabletop.hands.values().forEach { it.clear() }
	
	val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
	
	// Shuffle draw stack
	val seed = MathUtils.random.nextLong()
//	val seed = 9020568252116114615 // Starting hand with 8, A
//	val seed = -5000073366615045381 // Starting hand with 2
//	val seed = 2212245332158196130 // Starting hand with Q
//	val seed = -3202561125370556140 // Starting hand with A
	debug("Server | DEBUG") { "Shuffling with seed = $seed" }
	drawStack.shuffle(seed, tabletop)
	
	// Deal
	repeat(if (tabletop.hands.size > 2) 5 else 7) {
		for (username: String in tabletop.hands.orderedKeys())
			draw(drawStack.cards.peek(), username)
	}
	val topCard: ServerCard = drawStack.cards.peek()
	topCard.setServerCardGroup(discardPile, tabletop)
	topCard.isFaceUp = true
	
	// Set game state and action lock
	val playerHands = OrderedMap<Player, GdxArray<ServerCard>>()
	for ((username, hand) in tabletop.hands)
	{
		val user = tabletop.users[username]!!
		if (user.isAi)
			playerHands[IsmctsAgent(username)] = GdxArray(hand) as GdxArray<ServerCard>
		else
			playerHands[user] = GdxArray(hand) as GdxArray<ServerCard>
	}
	acquireActionLocks()
	serverGameState = ServerGameState(
		Ruleset(drawTwos = true, skips = true, reverses = true),
		playerHands,
		GdxArray(drawStack.cards),
		GdxArray(discardPile.cards)
	)
	serverGameState!!.onPlayerChanged = ::onPlayerChanged
	
	server.sendToAllTCP(Chat(message = "Game started", isSystemMessage = true))
	server.sendToAllTCP(NewGameEvent(cardGroupChangeEvent, seed, serverGameState!!.toGameState()))
	
	if (tabletop.users[serverGameState!!.currentPlayer.name].isAi)
	{
		KtxAsync.launch {
			while (true)
			{
				if (actionLocks.isEmpty)
					break
				skipFrame()
			}
			onPlayerChanged(serverGameState!!.currentPlayer)
		}
	}
}

fun Tabletop.onGameStateUpdated(gameState: GameState)
{
	val (_, players, currentPlayer, isPlayReversed, drawCount, declaredSuit, drawTwoEffectCardCount, powerCardPlayedEvent) = gameState
	val drawStackHolder = drawStackHolder!!
	room.gameState = gameState
	
	if (currentPlayer == game.user.name)
	{
		room.passButton.isVisible = drawCount >= 3 && powerCardPlayedEvent !is EightsPlayedEvent
		drawStackHolder.touchable =
			if (drawCount >= 3 || powerCardPlayedEvent is EightsPlayedEvent && powerCardPlayedEvent.playerUsername == game.user.name)
				Touchable.disabled
			else
				Touchable.enabled
		drawStackHolder.isFlashing = drawTwoEffectCardCount > 0
		if (drawTwoEffectCardCount > 0)
			myHand.setDarkened { it.rank != Rank.TWO }
		else
			myHand.setDarkened { powerCardPlayedEvent is EightsPlayedEvent && powerCardPlayedEvent.playerUsername == game.user.name }
	}
	else
	{
		room.passButton.isVisible = false
		drawStackHolder.touchable = Touchable.enabled
		drawStackHolder.isFlashing = false
		myHand.setDarkened { true }
	}
	
	if (players.size > 2 && MathUtils.isEqual(playDirectionIndicator.alpha, 0F))
	{
		playDirectionIndicator.scaleX = if (isPlayReversed) -1F else 1F
		playDirectionIndicator += fadeIn(2F)
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
			is ReversePlayedEvent -> onReversePlayed()
			is SkipsPlayedEvent -> onSkipsPlayed(powerCardPlayedEvent)
			else ->
			{
				for (actor: Actor in powerCardEffects.children)
				{
					actor.clearActions()
					actor += fadeOut(1F) then removeActor(actor)
				}
				
				if (drawTwoEffectCardCount > 0)
					powerCardEffects += EffectText(room, "+${drawTwoEffectCardCount}")
			}
		}
	}
}

object PassEvent

fun CrazyEightsServer.onPass()
{
	val serverGameState = serverGameState!!
	serverGameState.doMove(PassMove)
	server.sendToAllTCP(serverGameState.toGameState())
}

@NoArg
data class EightsPlayedEvent(val playerUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 0
}

fun Tabletop.onEightsPlayed(event: EightsPlayedEvent)
{
	room.dramatic.play()
	val suitChooser = SuitChooser(room, event.playerUsername == game.user.name)
	this.suitChooser = suitChooser
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		targeting(powerLabelGroup, fadeOut(0.5F)) along Actions.run { powerCardEffects += suitChooser }
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}

@NoArg
data class SuitDeclareEvent(val suit: Suit)

fun CrazyEightsServer.onSuitDeclare(connection: Connection? = null, event: SuitDeclareEvent)
{
	info("Server | INFO") { "Suit changed to ${event.suit.name}" }
	KtxAsync.launch {
		val serverGameState = serverGameState!!
		val topCard: ServerCard =
			(tabletop.idToObjectMap[tabletop.discardPileHolderId] as ServerCardHolder).cardGroup.cards.peek()
		kotlinx.coroutines.delay(1000)
		serverGameState.doMove(ChangeSuitMove(topCard, event.suit))
		server.sendToAllTCP(serverGameState.toGameState())
	}
	if (connection == null)
		server.sendToAllTCP(event)
	else
		server.sendToAllExceptTCP(connection.id, event)
}

@NoArg
data class DrawTwosPlayedEvent(val drawCardCount: Int) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

fun Tabletop.onDrawTwosPlayed(packet: DrawTwosPlayedEvent)
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			powerCardEffects += EffectText(room, "+${packet.drawCardCount}")
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}

@NoArg
data class DrawTwoPenaltyEvent(val victimUsername: String, val drawCardCount: Int)

fun Tabletop.onDrawTwoPenalty(event: DrawTwoPenaltyEvent)
{
	val (victimUsername, drawCardCount) = event
	val drawStackHolder = drawStackHolder!!
	val hand = userToHandMap[victimUsername]!!
	val drawTwoEffectText = powerCardEffects.children.firstOrNull { it is EffectText } as? EffectText
	drawTwoEffectText?.moveToHand(hand)
	
	drawStack!! += Actions.run {
		drawStackHolder.isFlashing = false
		drawStackHolder.touchable = Touchable.disabled
		myHand.touchable = Touchable.disabled
	} then delay(1.5F, DrawAction(room, hand, drawCardCount)) then Actions.run {
		drawStackHolder.touchable = Touchable.enabled
		myHand.touchable = Touchable.enabled
		game.client?.sendTCP(ActionLockReleaseEvent)
	}
}

@NoArg
data class SkipsPlayedEvent(val victimUsername: String) : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2500
}

fun Tabletop.onSkipsPlayed(event: SkipsPlayedEvent)
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			powerCardEffects += EffectText(room, "Q", userToHandMap[event.victimUsername]!!)
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
//	isPowerCardJustPlayed = true
}

object ReversePlayedEvent : PowerCardPlayedEvent
{
	override val delayMillis: Long
		get() = 2000
}

fun Tabletop.onReversePlayed()
{
	powerCardEffects.clearChildren()
	powerCardEffects += PowerCardEffect(room, discardPile!!.cards.peek() as Card) {
		defaultAction along Actions.run {
			persistentPowerCardEffects += PowerCardEffectRing(room)
			room.deepWhoosh.play()
			playDirectionIndicator.flipDirection()
		}
	}
	persistentPowerCardEffects += PowerCardEffectRing(room)
}
