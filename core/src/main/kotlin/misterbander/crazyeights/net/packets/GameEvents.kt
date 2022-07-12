package misterbander.crazyeights.net.packets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.OrderedMap
import com.esotericsoftware.kryonet.Connection
import kotlinx.coroutines.launch
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.KtxAsync
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.ai.IsmctsAgent
import misterbander.crazyeights.game.draw
import misterbander.crazyeights.game.resetDeck
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.GameState
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.EffectText
import misterbander.crazyeights.scene2d.Hand
import misterbander.crazyeights.scene2d.OpponentHand
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.actions.DealAction
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
	{
		playDirectionIndicator += fadeIn(2F)
		playDirectionIndicator.scaleX = 1F
	}
	
	room.gameState = gameState
}

@Suppress("UNCHECKED_CAST")
fun CrazyEightsServer.onNewGame(connection: Connection)
{
	if (actionLocks.isNotEmpty())
		return
	
	val drawStack = tabletop.drawStackHolder.cardGroup
	val discardPile = tabletop.discardPileHolder.cardGroup
	
	val seed = MathUtils.random.nextLong()
//	val seed = 9020568252116114615 // Starting hand with 8, A
//	val seed = -5000073366615045381 // Starting hand with 2
//	val seed = 2212245332158196130 // Starting hand with Q
//	val seed = -3202561125370556140 // Starting hand with A
//	val seed = 1505641440241536783 // First discard is 8
//	val seed = 1997011525088092652 // First discard is Q
	val cardGroupChangeEvent = tabletop.resetDeck(seed, true)
	
	// Deal
	repeat(if (tabletop.hands.size > 2) 5 else 7) {
		for (username: String in tabletop.hands.orderedKeys())
			draw(drawStack.cards.peek(), username, refillIfEmpty = false)
	}
	val topCard: ServerCard = drawStack.cards.peek()
	topCard.setServerCardGroup(tabletop, discardPile)
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
	val serverGameState = ServerGameState(ruleset, playerHands, GdxArray(drawStack.cards), GdxArray(discardPile.cards))
	this.serverGameState = serverGameState
	serverGameState.onPlayerChanged = ::onPlayerChanged
	val firstPlayer = serverGameState.currentPlayer.name
	
	server.sendToAllTCP(Chat(message = "${(connection.arbitraryData as User).name} started a new game", isSystemMessage = true))
	server.sendToAllTCP(NewGameEvent(cardGroupChangeEvent, seed, serverGameState.toGameState()))
	
	KtxAsync.launch {
		waitForActionLocks()
		val firstPower = serverGameState.triggerFirstPowerCard()
		when
		{
			ruleset.drawTwos != null && firstPower == ruleset.drawTwos ->
			{
				lastPowerCardPlayedEvent = DrawTwosPlayedEvent(serverGameState.drawTwoEffectCardCount)
				server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
			}
			ruleset.skips != null && firstPower == ruleset.skips ->
			{
				lastPowerCardPlayedEvent = SkipsPlayedEvent(firstPlayer)
				server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
			}
			ruleset.reverses != null && firstPower == ruleset.reverses && serverGameState.playerCount > 2 ->
			{
				lastPowerCardPlayedEvent = ReversePlayedEvent(serverGameState.isPlayReversed)
				server.sendToAllTCP(serverGameState.toGameState(lastPowerCardPlayedEvent))
			}
		}
		if (tabletop.users[serverGameState.currentPlayer.name].isAi)
			onPlayerChanged(serverGameState.currentPlayer)
	}
}

fun Tabletop.onGameStateUpdated(gameState: GameState)
{
	val (_, players, currentPlayer, isPlayReversed, drawCount, declaredSuit, drawTwoEffectCardCount, powerCardPlayedEvent) = gameState
	val drawStackHolder = drawStackHolder!!
	room.gameState = gameState
	
	if (currentPlayer == game.user.name)
	{
		room.passButton.isVisible = (drawCount >= gameState.ruleset.maxDrawCount || drawStack!!.cards.isEmpty)
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
			is ReversePlayedEvent -> onReversePlayed(powerCardPlayedEvent)
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

@NoArg
data class GameEndedEvent(val winner: String)

fun Tabletop.onGameEnded(event: GameEndedEvent)
{
	room.chatBox.chat("Game over! ${event.winner} won the game!", Color.YELLOW)
	room.passButton.isVisible = false
	drawStackHolder!!.touchable = Touchable.enabled
	drawStackHolder!!.isFlashing = false
	myHand.setDarkened { false }
	myHand.clearMemory()
	room.gameState = null
	for (hand: Hand in userToHandMap.values())
	{
		if (hand is OpponentHand && hand.user.isAi)
			hand.isHandOpen = true
	}
	playDirectionIndicator += fadeOut(2F)
	for (actor: Actor in powerCardEffects.children)
	{
		actor.clearActions()
		actor += fadeOut(1F) then removeActor(actor)
	}
}

object PassEvent
