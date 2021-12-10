package misterbander.crazyeights.net.packets

import misterbander.crazyeights.model.NoArg
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Tabletop

@NoArg
data class CardFlipEvent(val id: Int)

fun Tabletop.onCardFlip(event: CardFlipEvent)
{
	val card = idToGObjectMap[event.id] as Card
	card.isFaceUp = !card.isFaceUp
}
