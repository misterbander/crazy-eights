package misterbander.crazyeights.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import ktx.actors.minusAssign
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.User
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject

class CardHolder(
	private val room: RoomScreen,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	cardGroup: CardGroup? = null,
	val defaultType: ServerCardGroup.Type = cardGroup?.type ?: ServerCardGroup.Type.STACK,
	lockHolder: User? = null
) : GObject<CrazyEights>(room), DragTarget
{
	private val holderDrawable: Drawable = Scene2DSkin.defaultSkin["card_holder"]
	private val holderOverDrawable: Drawable = Scene2DSkin.defaultSkin["card_holder_over"]
	private val holderImage = scene2d.image(holderDrawable) { setPosition(0F, 0F, Align.center) }
	
	val cardGroup: CardGroup?
		get() = if (children.size > 1) children[1] as CardGroup else null
	
	private var flashAction: RepeatAction? = null
	var isFlashing: Boolean
		get() = flashAction != null
		set(value)
		{
			if (value)
			{
				flashAction = forever(
					Actions.run {
						highlightable.forceHighlight = true
					} then delay(
						0.5F, Actions.run { highlightable.forceHighlight = false }
					) then delay(0.5F)
				)
				this += flashAction!!
			}
			else if (flashAction != null)
			{
				this -= flashAction!!
				highlightable.forceHighlight = false
			}
		}
	
	// Modules
	private val smoothMovable = SmoothMovable(this, x, y, rotation)
	override val lockable: Lockable = object : Lockable(id, lockHolder, smoothMovable)
	{
		override val canTouchDown: Boolean
			get() = draggable.canDrag
	}
	private val draggable: Draggable = object : Draggable(room, smoothMovable, lockable)
	{
		override val canDrag: Boolean
			get() = false //this@CardHolder.cardGroup?.cards?.isEmpty != false
	}
	private val rotatable = Rotatable(smoothMovable, lockable, draggable)
	override val highlightable: Highlightable = object : Highlightable(this)
	{
		override val shouldHighlight: Boolean
			get() = super.shouldHighlight && this@CardHolder.cardGroup?.cards?.isEmpty != false || forceHighlight
		
		override val shouldExpand: Boolean
			get() = lockable.isLocked
	}
	
	init
	{
		this += holderImage
		if (cardGroup != null)
			this += cardGroup
		
		// Add modules
		this += smoothMovable
		this += lockable
		this += draggable
		this += rotatable
		this += highlightable
	}
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (super.hit(x, y, touchable) != null)
			return if (cardGroup?.cards?.isEmpty != false) this else cardGroup!!.hit(x, y, touchable)
		return null
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean =
		!room.isGameStarted && cardGroup != null && (gObject is Card || gObject is CardGroup)
	
	override fun accept(gObject: GObject<CrazyEights>) = cardGroup!!.accept(gObject)
	
	override fun toString(): String = "CardHolder(id=$id, cards=${children.size})"
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (highlightable.shouldHighlight)
		{
			batch.shader = room.brightenShader
			holderImage.drawable = holderOverDrawable
			super.draw(batch, parentAlpha)
			batch.shader = null
		}
		else
		{
			holderImage.drawable = holderDrawable
			super.draw(batch, parentAlpha)
		}
	}
}
