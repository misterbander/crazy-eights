package misterbander.crazyeights.scene2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Align
import ktx.actors.alpha
import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.scene2d.*
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.scene2d.SuitChooser.SuitButton.Type
import space.earlygrey.shapedrawer.ShapeDrawer
import space.earlygrey.shapedrawer.scene2d.ShapeDrawerDrawable

class SuitChooser(private val room: RoomScreen, isTouchable: Boolean) : Group()
{
	private val diamondsButton = SuitButton(this, Type.TOP_LEFT, Suit.DIAMONDS, room.game.shapeDrawer)
	private val clubsButton = SuitButton(this, Type.TOP_RIGHT, Suit.CLUBS, room.game.shapeDrawer)
	private val heartsButton = SuitButton(this, Type.BOTTOM_LEFT, Suit.HEARTS, room.game.shapeDrawer)
	private val spadesButton = SuitButton(this, Type.BOTTOM_RIGHT, Suit.SPADES, room.game.shapeDrawer)
	private val buttons = arrayOf(diamondsButton, clubsButton, heartsButton, spadesButton)
	
	var chosenSuit: Suit? = null
		set(value)
		{
			field = value
			if (value == null)
				return
			room.tabletop.persistentPowerCardEffects += PowerCardEffectRing(room)
			for (button in buttons)
			{
				button.clearActions()
				if (button.suit == value)
				{
					button.alpha = 1F
					button += delay(1F, fadeOut(0.2F))
				}
				else
					button.isVisible = false
			}
			touchable = Touchable.disabled
			this += scene2d.image(value.name.lowercase()) {
				setPosition(0F, 0F, Align.center)
				alpha = 0.7F
			}
		}
	
	init
	{
		val discardPileHolder = room.tabletop.discardPileHolder!!
		setPosition(discardPileHolder.x, discardPileHolder.y)
		
		this += scene2d.table {
			defaults().space(8F).size(140F)
			actor(diamondsButton)
			actor(clubsButton)
			row()
			actor(heartsButton)
			actor(spadesButton)
		}
		
		room.addUprightGObject(this)
		
		diamondsButton += fadeIn(0.2F)
		clubsButton += delay(0.25F, fadeIn(0.2F))
		heartsButton += delay(0.5F, fadeIn(0.2F))
		spadesButton += delay(0.75F, fadeIn(0.2F))
		
		touchable = if (isTouchable) Touchable.enabled else Touchable.disabled
	}
	
	class SuitButton(
		private val parent: SuitChooser,
		type: Type,
		val suit: Suit,
		shapeDrawer: ShapeDrawer
	): Container<Image>()
	{
		private val suitImage = scene2d.image(suit.name.lowercase()) { alpha = 0F }
		private val clickListener = onClick {
			parent.chosenSuit = suit
			parent.room.game.client?.sendTCP(SuitDeclareEvent(suit))
		}
		
		init
		{
			actor = suitImage
			background = object : ShapeDrawerDrawable(shapeDrawer)
			{
				override fun drawShapes(shapeDrawer: ShapeDrawer, x: Float, y: Float, width: Float, height: Float)
				{
					shapeDrawer.setColor(suitImage.color)
					when (type)
					{
						Type.TOP_RIGHT ->
							shapeDrawer.sector(x, y, width, 0F, 90*MathUtils.degRad)
						Type.TOP_LEFT ->
							shapeDrawer.sector(x + width, y, width, 90*MathUtils.degRad, 90*MathUtils.degRad)
						Type.BOTTOM_LEFT ->
							shapeDrawer.sector(x + width, y + width, width, 180*MathUtils.degRad, 90*MathUtils.degRad)
						Type.BOTTOM_RIGHT ->
							shapeDrawer.sector(x, y + width, width, 270*MathUtils.degRad, 90*MathUtils.degRad)
					}
				}
			}
			when (type)
			{
				Type.TOP_RIGHT -> pad(30F, 20F, 20F, 30F)
				Type.TOP_LEFT -> pad(30F, 30F, 20F, 20F)
				Type.BOTTOM_LEFT -> pad(20F, 30F, 30F, 20F)
				Type.BOTTOM_RIGHT -> pad(20F, 20F, 30F, 30F)
			}
			center()
			touchable = Touchable.enabled
			alpha = 0F
		}
		
		override fun act(delta: Float)
		{
			super.act(delta)
			suitImage.alpha = if (clickListener.isOver || parent.chosenSuit == suit) alpha else alpha/2
		}
		
		enum class Type
		{
			TOP_RIGHT, TOP_LEFT, BOTTOM_LEFT, BOTTOM_RIGHT
		}
	}
}
