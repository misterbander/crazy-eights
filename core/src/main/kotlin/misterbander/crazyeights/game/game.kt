package misterbander.crazyeights.game

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*
import ktx.log.debug
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.model.ServerLockable
import misterbander.crazyeights.model.ServerObject
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardSlideSoundEvent
import misterbander.crazyeights.net.packets.DrawStackRefillEvent
import misterbander.crazyeights.net.packets.DrawTwosPlayedEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.GameEndedEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ReversePlayedEvent
import misterbander.crazyeights.net.packets.SkipsPlayedEvent
import kotlin.math.round

fun CrazyEightsServer.play(cardGroupChangeEvent: CardGroupChangeEvent)
{
	val (cards, newCardGroupId, playerUsername) = cardGroupChangeEvent
	assert(cards.size == 1) { "Playing more than 1 card: $cards" }
	val discardPile = tabletop.idToObjectMap[newCardGroupId] as ServerCardGroup
	val card = tabletop.idToObjectMap[cards.first().id] as ServerCard
	val serverGameState = serverGameState!!
	val extraPackets = GdxArray<Any>()
	
	// Ignore if it's not the user's turn, or if the suit chooser is active and suit has not been declared yet,
	// or if not all action locks have been released
	if (playerUsername != serverGameState.currentPlayer.name
		|| serverGameState.declaredSuit == null && tabletop.suitChooser != null
		|| actionLocks.isNotEmpty())
		return
	val move = if (card.rank == Rank.EIGHT) ChangeSuitMove(card, Suit.DIAMONDS) else PlayMove(card)
	if (move !in serverGameState.moves)
		return
	when
	{
		card.rank == Rank.EIGHT ->
		{
			tabletop.suitChooser = playerUsername
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
		tabletop.suitChooser = null
	if (tabletop.hands[playerUsername]!!.removeValue(card, true))
		extraPackets += CardSlideSoundEvent
	card.rotation = cards[0].rotation
	card.isFaceUp = true
	card.setServerCardGroup(discardPile, tabletop)
	cards[0] = card
	runLater.getOrPut(playerUsername) { IntMap() }.remove(card.id)?.onCancel?.invoke()
	discardPile.arrange()
	server.sendToAllTCP(cardGroupChangeEvent)
	
	if (tabletop.hands[playerUsername].isEmpty) // Winner!
	{
		this.serverGameState = null
		aiJobs.forEach { it.cancel() }
		aiJobs.clear()
		if (CardSlideSoundEvent in extraPackets)
			server.sendToAllTCP(CardSlideSoundEvent)
		server.sendToAllTCP(GameEndedEvent(playerUsername))
		tabletop.suitChooser = null
		lastPowerCardPlayedEvent = null
		return
	}
	
	extraPackets.forEach { server.sendToAllTCP(it) }
	
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	if (drawStack.cards.isEmpty && card.rank != Rank.EIGHT)
		refillDrawStack()
}

fun CrazyEightsServer.draw(
	card: ServerCard,
	ownerUsername: String,
	fireOwnEvent: Boolean = false,
	playSound: Boolean = false,
	refillIfEmpty: Boolean = true
)
{
	card.isFaceUp = true
	card.setOwner(ownerUsername, tabletop)
	if (fireOwnEvent)
		server.sendToAllTCP(ObjectOwnEvent(card.id, ownerUsername))
	if (playSound)
		server.sendToAllTCP(CardSlideSoundEvent)
	
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	if (drawStack.cards.isEmpty && refillIfEmpty)
		refillDrawStack()
}

fun CrazyEightsServer.pass()
{
	val serverGameState = serverGameState!!
	serverGameState.doMove(PassMove)
	server.sendToAllTCP(serverGameState.toGameState())
	lastPowerCardPlayedEvent = null
}

fun CrazyEightsServer.refillDrawStack()
{
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	val discardPileHolder = tabletop.idToObjectMap[tabletop.discardPileHolderId] as ServerCardHolder
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
			discard.setServerCardGroup(drawStack, tabletop)
			discard.isFaceUp = false
		}
	}
	
	val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
	
	// Shuffle draw stack
	val seed = MathUtils.random.nextLong()
	debug("Server | DEBUG") { "Shuffling with seed = $seed" }
	drawStack.shuffle(seed, tabletop)
	
	// Rearrange the top card nicely
	topCard.x = 0F
	topCard.y = 0F
	topCard.rotation = 180*round(topCard.rotation/180)
	
	// Set game state and action lock
	acquireActionLocks()
	serverGameState.drawStack.clear()
	serverGameState.drawStack += drawStack.cards
	serverGameState.discardPile.clear()
	serverGameState.discardPile += topCard
	serverGameState.currentPlayerHand.clear()
	tabletop.hands[serverGameState.currentPlayer.name]!!.forEach { serverGameState.currentPlayerHand += it as ServerCard }
	
	server.sendToAllTCP(DrawStackRefillEvent(cardGroupChangeEvent, seed))
}

fun CrazyEightsServer.resetDeck(seed: Long, removeOffline: Boolean): CardGroupChangeEvent
{
	val idToObjectMap = tabletop.idToObjectMap
	val drawStack = (idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	
	// Recall all cards
	val serverObjects = idToObjectMap.values().toArray()
	for (serverObject: ServerObject in serverObjects) // Unlock everything and move all cards to the draw stack
	{
		if (serverObject is ServerLockable)
			serverObject.lockHolder = null
		if (serverObject is ServerCard)
		{
			if (serverObject.cardGroupId != drawStack.id)
				serverObject.setServerCardGroup(drawStack, tabletop)
			serverObject.isFaceUp = false
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
		for (username in tabletop.hands.orderedKeys().toArray(String::class.java)) // Remove hands of offline users
		{
			if (username !in tabletop.users)
				tabletop.hands.remove(username)
		}
	}
	tabletop.hands.values().forEach { it.clear() }
	
	val cardGroupChangeEvent = CardGroupChangeEvent(GdxArray(drawStack.cards), drawStack.id, "")
	
	// Shuffle draw stack
	debug("Server | DEBUG") { "Shuffling with seed = $seed" }
	drawStack.shuffle(seed, tabletop)
	
	return cardGroupChangeEvent
}
