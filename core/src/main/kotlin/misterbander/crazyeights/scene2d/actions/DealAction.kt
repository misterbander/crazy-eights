package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import ktx.actors.plusAssign
import ktx.actors.then
import misterbander.crazyeights.Room
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup

class DealAction(private val room: Room, private val hands: Array<CardGroup>) : RunnableAction()
{
	private var finished = false
	
	override fun act(delta: Float): Boolean
	{
		super.act(delta)
		return finished
	}
	
	override fun run()
	{
		val drawStack = room.tabletop.drawStackHolder.cardGroup!!
		val size = hands.size
		var i = 0
		
		actor += repeat(size*if (size > 2) 5 else 7, delay(0.1F, Actions.run {
			room.cardSlide.play()
			val card = drawStack.cards.peek() as Card
			val hand = hands[i%size]
			card.cardGroup = hand
			if (hand.ownable.hand != null)
			{
				hand.ownable.hand!!.arrange(false)
				card.isFaceUp = true
				card.ownable.wasInHand = true
			}
			else
				hand.arrange()
			i++
		})) then Actions.run { finished = true }
	}
}
