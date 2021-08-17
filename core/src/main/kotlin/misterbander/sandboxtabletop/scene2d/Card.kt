package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.GObject
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.ServerCard.Rank
import misterbander.sandboxtabletop.model.ServerCard.Suit
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.FlipCardEvent

class Card(
	screen: RoomScreen,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	rank: Rank = Rank.NO_RANK,
	suit: Suit = Suit.NO_SUIT,
	isFaceUp: Boolean = false,
	lockHolder: User? = null
) : GObject<SandboxTabletop>(screen)
{
	private val faceUpDrawable: Drawable = Scene2DSkin.defaultSkin[if (suit == Suit.JOKER) "cardjoker" else "card${suit}${rank}"]
	private val faceDownDrawable: Drawable = Scene2DSkin.defaultSkin["cardbackred"]
	private val cardImage = scene2d.image(if (isFaceUp) faceUpDrawable else faceDownDrawable) {
		setPosition(0F, 0F, Align.center)
	}
	
	private val clickListener = onClick {}
	
	// Modules
	private val smoothMovable = SmoothMovable(this, x, y, rotation)
	private val lockable: Lockable = Lockable(id, lockHolder, smoothMovable) {
		if (draggable.justDragged)
			draggable.justDragged = false
		else
			Network.client?.sendTCP(FlipCardEvent(id))
	}
	private val draggable = Draggable(lockable, clickListener, smoothMovable)
	
	init
	{
		this += cardImage
		
		// Add modules
		this += smoothMovable
		this += lockable
		this += draggable
	}
	
	var isFaceUp: Boolean = isFaceUp
		set(value)
		{
			field = value
			cardImage.drawable = if (value) faceUpDrawable else faceDownDrawable
		}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (clickListener.isOver)
		{
			batch.shader = (screen as RoomScreen).brightenShader
			super.draw(batch, parentAlpha)
			batch.shader = null
		}
		else
			super.draw(batch, parentAlpha)
	}
}
