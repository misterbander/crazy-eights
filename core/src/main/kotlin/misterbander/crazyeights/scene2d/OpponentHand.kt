package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.actors.txt
import ktx.collections.*
import ktx.graphics.copy
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import misterbander.crazyeights.PLAYER_NAMETAG_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.gframework.util.tempVec
import space.earlygrey.shapedrawer.ShapeDrawer
import space.earlygrey.shapedrawer.scene2d.ShapeDrawerDrawable
import kotlin.math.max
import kotlin.math.min

class OpponentHand(
	private val room: Room,
	var realX: Float = 0F,
	var realY: Float = 0F,
	rotation: Float = 0F,
	user: User = User("", Color.LIGHT_GRAY),
) : Hand(room)
{
	private var xAdjustFactor = 1F
	private var yAdjustFactor = 1F
	
	override val cardGroup = CardGroup(room, spreadSeparation = 20F, type = ServerCardGroup.Type.SPREAD).apply {
		setScale(0.5F)
	}
	private val nameLabel = scene2d.label(user.name, PLAYER_NAMETAG_LABEL_STYLE_S) {
		color = user.color
		pack()
	}
	private val nameLabelContainer = scene2d.container(nameLabel) {
		background = object : ShapeDrawerDrawable(game.shapeDrawer)
		{
			override fun drawShapes(shapeDrawer: ShapeDrawer, x: Float, y: Float, width: Float, height: Float) =
				shapeDrawer.rectangle(x - 2.5F, y - 2.5F, width + 5, height + 5, yellow, 5F)
		}
		pack()
		setPosition(0F, 0F, Align.center)
	}
	private val nameGroup = Group().apply {
		this += nameLabelContainer
		this.rotation = -rotation
	}
	
	private val yellow = Color.YELLOW.copy(alpha = 0F)
	private var yellowTargetAlpha = 1F
	private var time = 0F
	
	var user: User = user
		set(value)
		{
			field = value
			nameLabel.txt = value.name
			nameLabel.color = value.color
			nameLabelContainer.pack()
			nameLabelContainer.setPosition(0F, 0F, Align.center)
		}
	
	// Modules
	private val highlightable = Highlightable(this)
	
	init
	{
		x = realX
		y = realY
		this.rotation = rotation
		
		addActor(cardGroup)
		this += nameGroup
		
		room.addUprightGObject(nameGroup)
		
		onClick {
			room.click.play()
			room.userDialog.show(this.user)
		}
		
		// Add modules
		this += highlightable
	}
	
	override fun update(delta: Float)
	{
		// Players are situated in a 'round' table
		// Adjust apparent positions so that hands are visible in a rectangular screen
		val (screenX, screenY) = stage.stageToScreenCoordinates(tempVec.set(realX, realY))
		val adjustedScreenX = (screenX - Gdx.graphics.width/2)*xAdjustFactor + Gdx.graphics.width/2
		val adjustedScreenY = (screenY - Gdx.graphics.height/2)*yAdjustFactor + Gdx.graphics.height/2
		val (adjustedX, adjustedY) = stage.screenToStageCoordinates(tempVec.set(adjustedScreenX, adjustedScreenY))
		setPosition(adjustedX, adjustedY)
		val viewport = stage.viewport as ExtendViewport
		yAdjustFactor = min(viewport.worldHeight/viewport.minWorldWidth, 1F)
		
		// Turn indicator
		time += delta
		time = time.mod(1F)
		yellowTargetAlpha = min(0.75F - time*(time - 1), 1F)
		yellow.a = if (room.gameState?.currentPlayer == user.name)
			yellowTargetAlpha
		else
			max(yellow.a - 1.5F*delta, 0F)
	}
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (super.hit(x, y, touchable) != null)
			return this
		return null
	}
	
	override fun arrange(sort: Boolean) = cardGroup.arrange()
	
	fun flatten()
	{
		for (i in cardGroup.cards.lastIndex downTo 0)
		{
			val groupable: Groupable<CardGroup> = cardGroup.cards[i]
			if (groupable !is CardGroup)
				continue
			cardGroup -= groupable
			val cards: Array<Card> = groupable.cards.toArray(Card::class.java)
			cards.forEachIndexed { index, card ->
				card.ownable.wasInHand = true
				card.isFaceUp = true
				cardGroup.insert(card, i + index)
			}
		}
		cardGroup.arrange(false)
	}
	
	override fun rotationChanged()
	{
		nameGroup.rotation = -rotation
		room.addUprightGObject(nameGroup)
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (highlightable.shouldHighlight)
		{
			batch.shader = room.brightenShader
			super.draw(batch, parentAlpha)
			batch.shader = null
		}
		else
			super.draw(batch, parentAlpha)
	}
}
