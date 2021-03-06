package misterbander.crazyeights.net.packets

import com.badlogic.gdx.utils.IntMap
import com.esotericsoftware.kryonet.Connection
import ktx.actors.plusAssign
import ktx.collections.*
import misterbander.crazyeights.game.play
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.ServerCardHolder
import misterbander.crazyeights.net.ServerTabletop
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

fun ServerTabletop.onCardGroupCreate(event: CardGroupCreateEvent)
{
	val cards = event.cards
	val (firstId, firstX, firstY, firstRotation) = cards[0]
	val cardGroup = ServerCardGroup(parent.newId(), firstX, firstY, firstRotation)
	val insertAtIndex = serverObjects.indexOfFirst { it.id == firstId }
	cards.forEachIndexed { index, (id, x, y, rotation) ->
		val card = idToObjectMap[id] as ServerCard
		serverObjects.removeValue(card, true)
		card.x = x
		card.y = y
		card.rotation = rotation
		cards[index] = card
		cardGroup.plusAssign(this, card)
	}
	cardGroup.arrange()
	addServerObject(cardGroup, insertAtIndex)
	parent.server.sendToAllTCP(event.copy(id = cardGroup.id))
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
			if (oldCardGroup != null)
				oldCardGroup.ownable.myHand?.arrange() ?: oldCardGroup.arrange()
		}
		newCardGroup?.arrange()
	}
}

fun ServerTabletop.onCardGroupChange(event: CardGroupChangeEvent)
{
	val (cards, newCardGroupId, changerUsername) = event
	val newCardGroup = if (newCardGroupId != -1) idToObjectMap[newCardGroupId] as ServerCardGroup else null
	
	if (parent.isGameStarted && newCardGroup?.cardHolderId == discardPileHolder.id) // User discards a card
	{
		parent.play(event)
		return
	}
	
	cards.forEachIndexed { index, (id, _, _, rotation) ->
		val card = idToObjectMap[id] as ServerCard
		card.rotation = rotation
		card.setServerCardGroup(this, newCardGroup)
		cards[index] = card
		if (parent.isGameStarted) // Cancel the run later which would send the card back to its original owner
			parent.runLater.getOrPut(changerUsername) { IntMap() }.remove(id)?.onCancel?.invoke()
	}
	newCardGroup?.arrange()
	parent.server.sendToAllTCP(event)
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

fun ServerTabletop.onCardGroupDetach(event: CardGroupDetachEvent)
{
	val (cardHolderId, _, changerUsername) = event
	val cardHolder = idToObjectMap[cardHolderId] as ServerCardHolder
	val cardGroup = cardHolder.cardGroup
	cardGroup.x = cardHolder.x
	cardGroup.y = cardHolder.y
	cardGroup.rotation = cardHolder.rotation
	cardGroup.cardHolderId = -1
	serverObjects += cardGroup
	val replacementCardGroup = ServerCardGroup(parent.newId(), type = cardHolder.defaultType)
	idToObjectMap[replacementCardGroup.id] = replacementCardGroup
	cardHolder.cardGroup = replacementCardGroup
	replacementCardGroup.cardHolderId = cardHolder.id
	parent.server.sendToAllTCP(CardGroupDetachEvent(cardHolderId, cardHolder.cardGroup.id, changerUsername))
}

@NoArg
data class CardGroupDismantleEvent(val id: Int)

fun ServerTabletop.onCardGroupDismantle(connection: Connection, event: CardGroupDismantleEvent)
{
	val cardGroup = idToObjectMap[event.id] as ServerCardGroup
	while (cardGroup.cards.isNotEmpty())
	{
		val card: ServerCard = cardGroup.cards.removeIndex(0)
		card.setServerCardGroup(this, null)
	}
	idToObjectMap.remove(cardGroup.id)
	serverObjects.removeValue(cardGroup, true)
	parent.server.sendToAllExceptTCP(connection.id, event)
}

@NoArg
data class CardGroupShuffleEvent(val id: Int, val seed: Long)
