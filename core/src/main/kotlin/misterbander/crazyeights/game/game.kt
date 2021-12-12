package misterbander.crazyeights.game

import com.badlogic.gdx.utils.IntMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ktx.async.KtxAsync
import ktx.collections.*
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardSlideSoundEvent
import misterbander.crazyeights.net.packets.DrawTwoPenaltyEvent
import misterbander.crazyeights.net.packets.DrawTwosPlayedEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ReversePlayedEvent
import misterbander.crazyeights.net.packets.SkipsPlayedEvent

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
	extraPackets.forEach { server.sendToAllTCP(it) }
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
}

fun CrazyEightsServer.drawTwoPenalty(drawerUsername: String)
{
	val serverGameState = serverGameState!!
	val drawStack = (tabletop.idToObjectMap[tabletop.drawStackHolderId] as ServerCardHolder).cardGroup
	
	acquireActionLocks()
	repeat(serverGameState.drawTwoEffectCardCount) { draw(drawStack.cards.peek(), drawerUsername) }
	server.sendToAllTCP(DrawTwoPenaltyEvent(drawerUsername, serverGameState.drawTwoEffectCardCount))
	lastPowerCardPlayedEvent = null
	
	KtxAsync.launch {
		delay(1500L + serverGameState.drawTwoEffectCardCount*100)
		serverGameState.doMove(DrawTwoEffectPenalty(serverGameState.drawTwoEffectCardCount))
		server.sendToAllTCP(serverGameState.toGameState())
	}
}
