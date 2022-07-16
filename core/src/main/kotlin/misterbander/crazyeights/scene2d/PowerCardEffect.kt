package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.Align
import ktx.actors.along
import ktx.actors.alpha
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.scene2d.*
import misterbander.crazyeights.DESATURATED_RED
import misterbander.crazyeights.POWER_CARD_LABEL_STYLE
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.model.ServerCard.Suit

class PowerCardEffect(
	room: RoomScreen,
	card: Card,
	action: PowerCardEffect.() -> Action = { defaultAction }
) : Group()
{
	val shineImage = scene2d.image("shine") {
		setPosition(0F, 0F, Align.center)
		setOrigin(Align.center)
		setScale(2.6F)
		alpha = 0F
	}
	val powerLabel = scene2d.label(card.rank.toString(), POWER_CARD_LABEL_STYLE) {
		pack()
		setPosition(0F, 0F, Align.center)
	}
	val powerLabelGroup = Group().apply {
		this += powerLabel
		setScale(0F)
	}
	val defaultAction: Action
		get() = targeting(shineImage, fadeOut(0.5F)) along targeting(powerLabelGroup, fadeOut(0.5F)) then
			Actions.removeActor(this)
	
	init
	{
		val discardPileHolder = room.tabletop.discardPileHolder!!
		setPosition(discardPileHolder.x, discardPileHolder.y)
		
		this += shineImage
		this += powerLabelGroup
		touchable = Touchable.disabled
		
		shineImage += fadeIn(0.7F)
		powerLabelGroup += scaleTo(1F, 1F, 0.5F, Interpolation.swingOut)
		powerLabel += color(if (card.suit == Suit.DIAMONDS || card.suit == Suit.HEARTS) DESATURATED_RED else Color.BLACK, 0.5F)
		
		room.addUprightGObject(this)
		
		this += delay(1F, action())
	}
}
