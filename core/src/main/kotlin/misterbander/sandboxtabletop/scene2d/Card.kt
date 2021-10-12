package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.GObject
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.ServerCard.Rank
import misterbander.sandboxtabletop.model.ServerCard.Suit
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.FlipCardEvent
import misterbander.sandboxtabletop.scene2d.modules.Draggable
import misterbander.sandboxtabletop.scene2d.modules.Highlightable
import misterbander.sandboxtabletop.scene2d.modules.Lockable
import misterbander.sandboxtabletop.scene2d.modules.Rotatable
import misterbander.sandboxtabletop.scene2d.modules.SmoothMovable

class Card(
	private val room: Room,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	rank: Rank = Rank.NO_RANK,
	suit: Suit = Suit.NO_SUIT,
	isFaceUp: Boolean = false,
	lockHolder: User? = null
) : GObject<SandboxTabletop>(room)
{
	private val faceUpDrawable: Drawable = Scene2DSkin.defaultSkin[if (suit == Suit.JOKER) "cardjoker" else "card${suit}${rank}"]
	private val faceDownDrawable: Drawable = Scene2DSkin.defaultSkin["cardbackred"]
	private val cardImage = scene2d.image(if (isFaceUp) faceUpDrawable else faceDownDrawable) {
		setPosition(0F, 0F, Align.center)
	}
	
	// Modules
	private val smoothMovable = SmoothMovable(this, x, y, rotation)
	private val lockable: Lockable = object : Lockable(id, lockHolder, smoothMovable)
	{
		override fun unlock()
		{
			if (isLockHolder && !draggable.justDragged && !rotatable.justRotated)
				game.client?.sendTCP(FlipCardEvent(id))
			super.unlock()
		}
	}
	private val draggable = Draggable(smoothMovable, lockable)
	private val rotatable = Rotatable(smoothMovable, lockable, draggable)
	private val highlightable = Highlightable(smoothMovable, lockable)
	
	init
	{
		this += cardImage
		
		// Add modules
		this += smoothMovable
		this += lockable
		this += draggable
		this += rotatable
		this += highlightable
	}
	
	var isFaceUp: Boolean = isFaceUp
		set(value)
		{
			field = value
			cardImage.drawable = if (value) faceUpDrawable else faceDownDrawable
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
