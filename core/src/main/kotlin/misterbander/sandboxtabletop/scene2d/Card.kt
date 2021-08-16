package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import ktx.actors.onClick
import ktx.actors.plusAssign
import ktx.collections.plusAssign
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.GObject
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.ServerCard
import misterbander.sandboxtabletop.model.ServerCard.Rank
import misterbander.sandboxtabletop.model.ServerCard.Suit

class Card(
	screen: RoomScreen,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	val rank: Rank = Rank.NO_RANK,
	val suit: Suit = Suit.NO_SUIT,
	isFaceUp: Boolean = false
) : GObject<SandboxTabletop>(screen)
{
	private val faceUpDrawable: Drawable = Scene2DSkin.defaultSkin[if (suit == Suit.JOKER) "cardjoker" else "card${suit}${rank}"]
	private val faceDownDrawable: Drawable = Scene2DSkin.defaultSkin["cardbackred"]
	private val cardImage = scene2d.image(if (isFaceUp) faceUpDrawable else faceDownDrawable) {
		setPosition(0F, 0F, Align.center)
	}
	
	private val clickListener = onClick {}
	
	// Modules
	val smoothMovable = SmoothMovable(this, x, y, rotation)
	
	init
	{
		this += cardImage
		
		// Add modules
		modules += smoothMovable
		modules += Draggable(smoothMovable, clickListener)
	}
	
	var isFaceUp: Boolean = isFaceUp
		set(value)
		{
			field = value
			cardImage.drawable = if (value) faceUpDrawable else faceDownDrawable
		}
	
	val serverCard: ServerCard
		get() = ServerCard(id, x, y, rotation, rank, suit, isFaceUp)
	
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
