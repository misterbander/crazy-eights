package misterbander.crazyeights.scene2d.actions

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import ktx.actors.along
import ktx.actors.plusAssign
import ktx.actors.then
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.gframework.util.shuffle

class ShuffleAction(
	private val room: RoomScreen,
	private val seed: Long
) : RunnableAction()
{
	private var finished = false
	
	override fun act(delta: Float): Boolean
	{
		super.act(delta)
		return finished
	}
	
	override fun run()
	{
		val cardGroup = actor as CardGroup
		cardGroup.cardHolder?.toFront() ?: cardGroup.toFront()
		
		val cards = cardGroup.cards
		cards.shuffle(seed)
		var i = 0
		var j = 0
		
		cardGroup += repeat(
			cards.size, delay(0.01F, Actions.run {
				val card = room.tabletop.idToGObjectMap[(cards[i] as Card).id] as Card
				if (i%2 == 0)
					room.cardSlide.play()
				card.smoothMovable.x += if (i%2 == 0) -150 else 150
				i++
			})
		) along delay(
			0.2F, repeat(cards.size, delay(0.01F, Actions.run {
					val card = room.tabletop.idToGObjectMap[(cards[j] as Card).id] as Card
					card.smoothMovable.setPosition(-j.toFloat(), j.toFloat())
					card.zIndex = j
					j++
				})
			)
		) then Actions.run { finished = true }
	}
}
