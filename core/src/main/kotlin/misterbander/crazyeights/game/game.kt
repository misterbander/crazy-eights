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
		serverGameState.ruleset.drawTwos && card.rank == Rank.TWO ->
		{
			serverGameState.doMove(move)
			lastPowerCardPlayedEvent = DrawTwosPlayedEvent(serverGameState.drawTwoEffectCardCount)
			extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
		}
		serverGameState.ruleset.skips && card.rank == Rank.QUEEN ->
		{
			lastPowerCardPlayedEvent = SkipsPlayedEvent(serverGameState.nextPlayer.name)
			serverGameState.doMove(move)
			extraPackets += serverGameState.toGameState(lastPowerCardPlayedEvent)
		}
		serverGameState.ruleset.reverses && card.rank == Rank.ACE && serverGameState.playerCount > 2 ->
		{
			serverGameState.doMove(move)
			lastPowerCardPlayedEvent = ReversePlayedEvent
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
	
	if (serverGameState.isTerminal)
	{
		var winner: Player = serverGameState.playerHands.orderedKeys().first()
		for (player: Player in serverGameState.playerHands.orderedKeys())
		{
			if (serverGameState.getResult(player) == 1)
				winner = player
		}
		this.serverGameState = null
		aiJob?.cancel()
		if (CardSlideSoundEvent in extraPackets)
			server.sendToAllTCP(CardSlideSoundEvent)
		server.sendToAllTCP(GameEndedEvent(winner.name))
		return
	}
	
	extraPackets.forEach { server.sendToAllTCP(it) }
	
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	if (drawStack.cards.isEmpty)
		refillDrawStack()
}

fun CrazyEightsServer.draw(
	card: ServerCard,
	ownerUsername: String,
	fireOwnEvent: Boolean = false,
	playSound: Boolean = false
)
{
	card.isFaceUp = true
	card.setOwner(ownerUsername, tabletop)
	if (fireOwnEvent)
		server.sendToAllTCP(ObjectOwnEvent(card.id, ownerUsername))
	if (playSound)
		server.sendToAllTCP(CardSlideSoundEvent)
	
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	if (drawStack.cards.isEmpty)
		refillDrawStack()
}

fun CrazyEightsServer.pass()
{
	val serverGameState = serverGameState!!
	serverGameState.doMove(PassMove)
	server.sendToAllTCP(serverGameState.toGameState())
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
	
	server.sendToAllTCP(DrawStackRefillEvent(cardGroupChangeEvent, seed))
}