package misterbander.crazyeights.net.packets

import misterbander.crazyeights.Room
import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.scene2d.Card

@NoArg
data class CardFlipEvent(val id: Int)

fun Room.onCardFlip(event: CardFlipEvent)
{
	val card = tabletop.idToGObjectMap[event.id] as Card
	card.isFaceUp = !card.isFaceUp
}
