package misterbander.crazyeights.net.packets

import com.badlogic.gdx.utils.IntMap
import com.esotericsoftware.kryonet.Connection
import ktx.actors.plusAssign
import ktx.collections.*
import misterbander.crazyeights.Room
import misterbander.crazyeights.game.PlayMove
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.net.CrazyEightsServer
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.CardHolder
import misterbander.crazyeights.scene2d.transformToGroupCoordinates

@NoArg
data class CardGroupCreateEvent(val id: Int = -1, val cards: GdxArray<ServerCard>)

fun Room.onCardGroupCreate(event: CardGroupCreateEvent)
{
	val (id, serverCards) = event
	val cards = serverCards.map { tabletop.idToGObjectMap[it.id] as Card }
	val firstX = cards.first().smoothMovable.x
	val firstY = cards.first().smoothMovable.y
	val firstRotation = cards.first().smoothMovable.rotation
	val cardGroup = CardGroup(this, id, firstX, firstY, firstRotation)
	tabletop.cards.addActorAfter(cards.first(), cardGroup)
	cards.forEachIndexed { index, card: Card ->
		val (_, x, y, rotation) = serverCards[index]
		cardGroup += card
		card.smoothMovable.setPosition(x, y)
		card.smoothMovable.rotation = rotation
	}
	cardGroup.arrange()
	tabletop.idToGObjectMap[id] = cardGroup
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

fun Room.onCardGroupChange(event: CardGroupChangeEvent)
{
	val (cards, newCardGroupId, changerUsername) = event
	if (changerUsername != game.user.name || newCardGroupId != -1)
	{
		val newCardGroup = if (newCardGroupId != -1) tabletop.idToGObjectMap[newCardGroupId] as CardGroup else null
		for ((id, x, y, rotation) in cards)
		{
			val card = tabletop.idToGObjectMap[id] as Card
			val oldCardGroup = card.cardGroup
			card.cardGroup = newCardGroup
			card.smoothMovable.setPosition(x, y)
			card.smoothMovable.rotation = rotation
			oldCardGroup?.arrange()
		}
		newCardGroup?.arrange()
	}
}

fun CrazyEightsServer.onCardGroupChange(event: CardGroupChangeEvent)
{
	val (cards, newCardGroupId, changerUsername) = event
	val newCardGroup = if (newCardGroupId != -1) tabletop.idToObjectMap[newCardGroupId] as ServerCardGroup else null
	var shouldPlayCardSlideSound = false
	
	var isEightsPlayed = false
	var isDrawTwosPlayed = false
	var isSkipsPlayed = false
	
	cards.forEachIndexed { index, (id, _, _, rotation) ->
		val card = tabletop.idToObjectMap[id] as ServerCard
		if (isGameStarted && newCardGroup?.cardHolderId == tabletop.discardPileHolderId)
		{
			assert(index == 0) { "Playing more than 1 card: $cards" }
			val move = PlayMove(card)
			if (card.rank == Rank.EIGHT)
			{
				tabletop.suitChooser = changerUsername
				isEightsPlayed = true
			}
			else if (move !in serverGameState!!.moves)
				return
			when (card.rank)
			{
				Rank.TWO -> isDrawTwosPlayed = true
				Rank.QUEEN -> isSkipsPlayed = true
				else -> {}
			}
		}
		card.rotation = rotation
		shouldPlayCardSlideSound = shouldPlayCardSlideSound || tabletop.hands[changerUsername]!!.removeValue(card, true)
		card.setServerCardGroup(newCardGroup, tabletop)
		cards[index] = card
		if (isGameStarted)
			runLater.getOrPut(changerUsername) { IntMap() }.remove(id)?.onCancel?.invoke()
	}
	newCardGroup?.arrange()
	server.sendToAllTCP(event)
	if (isGameStarted)
	{
		if (shouldPlayCardSlideSound)
			server.sendToAllTCP(CardSlideSoundEvent)
		when
		{
			isEightsPlayed -> server.sendToAllTCP(EightsPlayedEvent(changerUsername))
			isDrawTwosPlayed -> server.sendToAllTCP(DrawTwosPlayedEvent)
			isSkipsPlayed -> server.sendToAllTCP(SkipsPlayedEvent(serverGameState!!.nextPlayer.name))
		}
	}
}

@NoArg
data class CardGroupDetachEvent(val cardHolderId: Int, val replacementCardGroupId: Int = -1, val changerUsername: String)

fun Room.onCardGroupDetach(event: CardGroupDetachEvent)
{
	val (cardHolderId, replacementCardGroupId, changerUsername) = event
	val cardHolder = tabletop.idToGObjectMap[cardHolderId] as CardHolder
	if (changerUsername != game.user.name)
	{
		val cardGroup = cardHolder.cardGroup!!
		cardGroup.transformToGroupCoordinates(tabletop.cards)
		tabletop.cards += cardGroup
	}
	val replacementCardGroup = CardGroup(this, replacementCardGroupId, type = cardHolder.defaultType)
	tabletop.idToGObjectMap[replacementCardGroupId] = replacementCardGroup
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
