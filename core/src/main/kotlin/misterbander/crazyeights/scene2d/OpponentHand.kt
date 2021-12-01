package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.PLAYER_NAMETAG_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec
import kotlin.math.min

class OpponentHand(
	private val room: Room,
	var realX: Float = 0F,
	var realY: Float = 0F,
	rotation: Float = 0F,
	user: User,
) : GObject<CrazyEights>(room)
{
	private var xAdjustFactor = 1F
	private var yAdjustFactor = 1F
	
	private val cardGroup = CardGroup(room, spreadSeparation = 20F, type = ServerCardGroup.Type.SPREAD).apply {
		setScale(0.5F)
	}
	private val nameGroup = Group().apply {
		this += scene2d.label(user.username, PLAYER_NAMETAG_LABEL_STYLE_S) {
			pack()
			color = user.color
			setPosition(0F, 0F, Align.center)
		}
		setPosition(0F, 0F, Align.center)
		this.rotation = -rotation
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
			room.userDialog.show(user)
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
	}
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (super.hit(x, y, touchable) != null)
			return this
		return null
	}
	
	operator fun plusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup += groupable
	}
	
	operator fun minusAssign(groupable: Groupable<CardGroup>)
	{
		cardGroup -= groupable
	}
	
	fun arrange() = cardGroup.arrange()
	
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
