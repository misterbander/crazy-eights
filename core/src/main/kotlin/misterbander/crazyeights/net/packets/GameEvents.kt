package misterbander.crazyeights.net.packets

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.along
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.*
import ktx.log.info
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.game.Player
import misterbander.crazyeights.game.ServerGameState
import misterbander.crazyeights.game.ai.IsmctsAgent
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.GameState
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Hand
import misterbander.crazyeights.scene2d.PowerCardEffect
import misterbander.crazyeights.scene2d.PowerCardEffectRing
import misterbander.crazyeights.scene2d.SuitChooser
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

object NewGameActionFinishedEvent

fun Room.onNewGame(event: NewGameEvent)
{
	val (cardGroupChangeEvent, shuffleSeed, gameState) = event
	for (gObject: GObject<CrazyEights> in tabletop.idToGObjectMap.values()) // Unlock and disown everything
	{
		gObject.getModule<Lockable>()?.unlock(false)
		gObject.getModule<Ownable>()?.wasInHand = false
	}
	onCardGroupChange(cardGroupChangeEvent!!)
	val drawStack = tabletop.drawStack!!
	drawStack.flip(false)
	
	val userToHandMap = tabletop.userToHandMap
	for ((username, hand) in userToHandMap.toGdxArray()) // Remove hands of offline users
	{
		if (username !in tabletop.users)
		{
			userToHandMap.remove(username)
			hand!!.remove()
		}
	}
	tabletop.arrangePlayers()
	
	val hands: Array<Hand> = userToHandMap.orderedKeys().map { userToHandMap[it]!! }.toArray(Hand::class.java)
	
	drawStack += Actions.run {
		tabletop.drawStackHolder!!.touchable = Touchable.disabled
		tabletop.myHand.touchable = Touchable.disabled
	} then
		delay(1F, ShowCenterTitleAction(this, "Shuffling...")) then
		ShuffleAction(this, shuffleSeed) then
		delay(0.5F, ShowCenterTitleAction(this, "Dealing...")) then
		DealAction(this, hands) then
		HideCenterTitleAction(this) then
		Actions.run {
			tabletop.drawStackHolder!!.touchable = Touchable.enabled
			tabletop.myHand.touchable = Touchable.enabled
			game.client?.sendTCP(NewGameActionFinishedEvent)
		}
	
	this.gameState = gameState
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
	drawStack.shuffle(seed, tabletop)
	
	// Deal
	repeat(if (tabletop.hands.size > 2) 5 else 7) {
		for (username: String in tabletop.hands.orderedKeys())
			drawStack.draw(username)
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
		{
			playerHands[user] = GdxArray(hand) as GdxArray<ServerCard>
			actionLocks += user.name
		}
	}
	serverGameState = ServerGameState(
		playerHands = playerHands,
		drawStack = GdxArray(drawStack.cards),
		discardPile = GdxArray(discardPile.cards)
	)
	
	server.sendToAllTCP(Chat(message = "Game started", isSystemMessage = true))
	server.sendToAllTCP(NewGameEvent(cardGroupChangeEvent, seed, serverGameState!!.toGameState()))
}

@NoArg
data class EightsPlayedEvent(val playerUsername: String)

fun Room.onEightsPlayed(event: EightsPlayedEvent)
{
	dramatic.play()
	val suitChooser = SuitChooser(this@onEightsPlayed, event.playerUsername == game.user.name)
	tabletop.suitChooser = suitChooser
	tabletop.effects += PowerCardEffect(this, tabletop.discardPile!!.cards.peek() as Card) {
		targeting(powerLabelGroup, fadeOut(0.5F)) along Actions.run { tabletop.effects += suitChooser }
	}
	tabletop.effects += PowerCardEffectRing(this)
}

@NoArg
data class SuitDeclareEvent(val suit: Suit)

fun CrazyEightsServer.onSuitDeclare(event: SuitDeclareEvent)
{
	info("Server | INFO") { "Suit changed to ${event.suit.name}" }
	server.sendToAllTCP(event)
}
