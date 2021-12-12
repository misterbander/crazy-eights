package misterbander.crazyeights.net.packets

import com.badlogic.gdx.utils.IntMap
import com.esotericsoftware.kryonet.Connection
import ktx.actors.plusAssign
import ktx.collections.*
import misterbander.crazyeights.game.ChangeSuitMove
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.CardHolder
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.transformToGroupCoordinates

@NoArg
data class CardGroupCreateEvent(val id: Int = -1, val cards: GdxArray<ServerCard>)

fun Tabletop.onCardGroupCreate(event: CardGroupCreateEvent)
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

fun CrazyEightsServer.onCardGroupCreate(event: CardGroupCreateEvent)
{
	val cards = event.cards
	val (firstId, firstX, firstY, firstRotation) = cards[0]
	val cardGroup = ServerCardGroup(newId(), firstX, firstY, firstRotation)
	val insertAtIndex = tabletop.serverObjects.indexOfFirst { it.id == firstId }
	cards.forEachIndexed { index, (id, x, y, rotation) ->
		val card = tabletop.idToObjectMap[id] as ServerCard
		tabletop.serverObjects.removeValue(card, true)
		card.x = x
		card.y = y
		card.rotation = rotation
		cards[index] = card
		cardGroup.plusAssign(card, tabletop)
	}
	cardGroup.arrange()
	tabletop.addServerObject(cardGroup, insertAtIndex)
	server.sendToAllTCP(event.copy(id = cardGroup.id))
}

@NoArg
data class CardGroupChangeEvent(val cards: GdxArray<ServerCard>, val newCardGroupId: Int, val changerUsername: String)

fun Tabletop.onCardGroupChange(event: CardGroupChangeEvent)
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
			oldCardGroup?.arrange()
		}
		newCardGroup?.arrange()
	}
}

fun CrazyEightsServer.onCardGroupChange(event: CardGroupChangeEvent)
{
	val (cards, newCardGroupId, changerUsername) = event
	val newCardGroup = if (newCardGroupId != -1) tabletop.idToObjectMap[newCardGroupId] as ServerCardGroup else null
	val extraPackets = GdxArray<Any>()
	
	cards.forEachIndexed { index, (id, _, _, rotation) ->
		val card = tabletop.idToObjectMap[id] as ServerCard
		if (isGameStarted && newCardGroup?.cardHolderId == tabletop.discardPileHolderId) // Users discards a card
		{
			assert(index == 0) { "Playing more than 1 card: $cards" }
			val serverGameState = serverGameState!!
			// Ignore if it's not the user's turn, or if the suit chooser is active and suit has not been declared yet,
			// or if not all action locks have been released
			if (changerUsername != serverGameState.currentPlayer.name
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
					tabletop.suitChooser = changerUsername
					extraPackets += serverGameState.toGameState(EightsPlayedEvent(changerUsername))
				}
				serverGameState.ruleset.drawTwos && card.rank == Rank.TWO ->
				{
					serverGameState.doMove(move)
					extraPackets += serverGameState.toGameState(DrawTwosPlayedEvent(serverGameState.drawTwoEffectCardCount))
				}
				serverGameState.ruleset.skips && card.rank == Rank.QUEEN ->
				{
					val skipsPlayedEvent = SkipsPlayedEvent(serverGameState.nextPlayer.name)
					serverGameState.doMove(move)
					extraPackets += serverGameState.toGameState(skipsPlayedEvent)
				}
				serverGameState.ruleset.reverses && card.rank == Rank.ACE && serverGameState.playerCount > 2 ->
				{
					serverGameState.doMove(move)
					extraPackets += serverGameState.toGameState(ReversePlayedEvent)
				}
				else ->
				{
					serverGameState.doMove(move)
					extraPackets += serverGameState.toGameState()
				}
			}
			if (card.rank != Rank.EIGHT)
				tabletop.suitChooser = null
			if (tabletop.hands[changerUsername]!!.removeValue(card, true))
				extraPackets += CardSlideSoundEvent
		}
		card.rotation = rotation
		card.setServerCardGroup(newCardGroup, tabletop)
		cards[index] = card
		if (isGameStarted)
			runLater.getOrPut(changerUsername) { IntMap() }.remove(id)?.onCancel?.invoke()
	}
	newCardGroup?.arrange()
	server.sendToAllTCP(event)
	extraPackets.forEach { server.sendToAllTCP(it) }
}

@NoArg
data class CardGroupDetachEvent(val cardHolderId: Int, val replacementCardGroupId: Int = -1, val changerUsername: String)

fun Tabletop.onCardGroupDetach(event: CardGroupDetachEvent)
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

fun CrazyEightsServer.onCardGroupDetach(event: CardGroupDetachEvent)
{
	val (cardHolderId, _, changerUsername) = event
	val cardHolder = tabletop.idToObjectMap[cardHolderId] as ServerCardHolder
	val cardGroup = cardHolder.cardGroup
	cardGroup.x = cardHolder.x
	cardGroup.y = cardHolder.y
	cardGroup.rotation = cardHolder.rotation
	cardGroup.cardHolderId = -1
	tabletop.serverObjects += cardGroup
	val replacementCardGroup = ServerCardGroup(newId(), type = cardHolder.defaultType)
	tabletop.idToObjectMap[replacementCardGroup.id] = replacementCardGroup
	cardHolder.cardGroup = replacementCardGroup
	replacementCardGroup.cardHolderId = cardHolder.id
	server.sendToAllTCP(CardGroupDetachEvent(cardHolderId, cardHolder.cardGroup.id, changerUsername))
}

@NoArg
data class CardGroupDismantleEvent(val id: Int)

fun CrazyEightsServer.onCardGroupDismantle(connection: Connection, event: CardGroupDismantleEvent)
{
	val cardGroup = tabletop.idToObjectMap[event.id] as ServerCardGroup
	while (cardGroup.cards.isNotEmpty())
	{
		val card: ServerCard = cardGroup.cards.removeIndex(0)
		card.setServerCardGroup(null, tabletop)
	}
	tabletop.idToObjectMap.remove(cardGroup.id)
	tabletop.serverObjects.removeValue(cardGroup, true)
	server.sendToAllExceptTCP(connection.id, event)
}

@NoArg
data class CardGroupShuffleEvent(val id: Int, val seed: Long)
