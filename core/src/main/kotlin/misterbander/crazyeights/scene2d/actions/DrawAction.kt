package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import ktx.actors.plusAssign
import ktx.actors.then
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.Hand
import misterbander.crazyeights.scene2d.MyHand
import kotlin.math.min

class DrawAction(private val room: RoomScreen, private val hand: Hand, private val drawCount: Int) : RunnableAction()
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
		actor!! += repeat(min(drawCount, drawStack.cards.size), delay(0.1F, Actions.run {
			room.cardSlide.play()
			val card = drawStack.cards.peek() as Card
			card.cardGroup = hand.cardGroup
			if (hand is MyHand)
			{
				hand.arrange(false)
				card.isFaceUp = true
				card.ownable.wasInHand = true
				card.isDarkened = true
			}
			else
				hand.arrange()
		})) then Actions.run { finished = true }
	}
}
