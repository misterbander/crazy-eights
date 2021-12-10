package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import ktx.actors.plusAssign
import ktx.actors.then
import misterbander.crazyeights.Room
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Hand
import misterbander.crazyeights.scene2d.MyHand

class DealAction(private val room: Room, private val hands: Array<Hand>) : RunnableAction()
{
	private var finished = false
	
	override fun act(delta: Float): Boolean
	{
		super.act(delta)
		return finished
	}
	
	override fun run()
	{
		val drawStack = room.tabletop.drawStack!!
		val discardPile = room.tabletop.discardPile!!
		val size = hands.size
		var i = 0
		
		actor += repeat(size*if (size > 2) 5 else 7, delay(0.1F, Actions.run {
			room.cardSlide.play()
			val card = drawStack.cards.peek() as Card
			val hand = hands[i%size]
			card.cardGroup = hand.cardGroup
			if (hand is MyHand)
			{
				hand.arrange(false)
				card.isFaceUp = true
				card.ownable.wasInHand = true
			}
			else
				hand.arrange()
			i++
		})) then delay(0.1F, Actions.run {
			room.cardSlide.play()
			val topCard = drawStack.cards.peek() as Card
			topCard.cardGroup = discardPile
			topCard.smoothMovable.setTargetPosition(0F, 0F)
			topCard.smoothMovable.rotationInterpolator.target = 0F
			topCard.isFaceUp = true
			discardPile.arrange()
			finished = true
		})
	}
}
