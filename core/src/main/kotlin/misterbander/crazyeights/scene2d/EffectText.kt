package misterbander.crazyeights.scene2d

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.EFFECT_TEXT_LABEL_STYLE
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class EffectText(room: RoomScreen, text: String, targetHand: Hand? = null, delay: Float = 0.5F) : GObject<CrazyEights>(room)
{
	// Modules
	private val smoothMovable: SmoothMovable
	
	init
	{
		val discardPileHolder = room.tabletop.discardPileHolder
		setPosition(discardPileHolder.x, discardPileHolder.y)
		smoothMovable = SmoothMovable(this)
		setScale(0F)
		
		this += scene2d.label(text, EFFECT_TEXT_LABEL_STYLE) {
			pack()
			setPosition(0F, 0F, Align.center)
		}
		
		this += smoothMovable.apply {
			yInterpolator.smoothingFactor = 9F
			xInterpolator.smoothingFactor = 9F
		}
		
		room.addUprightGObject(this)
		
		if (targetHand != null)
			this += delay(delay, Actions.run { moveToHand(targetHand) })
	}
	
	fun moveToHand(hand: Hand)
	{
		val (x, y) = hand.cardGroup.localToStageCoordinates(tempVec.set(0F, if (hand is MyHand) hand.offsetCenterY else 0F))
		smoothMovable.setPosition(x, y)
		this += delay(1F, fadeOut(0.5F)) then Actions.removeActor(this)
	}
}
