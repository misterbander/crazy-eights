package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.collections.*
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCard
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject

class Card(
	val room: Room,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	private val rank: Rank = Rank.NO_RANK,
	private val suit: Suit = Suit.NO_SUIT,
	isFaceUp: Boolean = false,
	lockHolder: User? = null
) : GObject<CrazyEights>(room), DragTarget
{
	private val faceUpDrawable: Drawable =
		Scene2DSkin.defaultSkin[if (suit == Suit.JOKER) "cardjoker" else "card${suit.name.lowercase()}${if (rank == Rank.ACE || rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING) rank.name.lowercase() else rank}"]
	private val faceDownDrawable: Drawable = Scene2DSkin.defaultSkin["cardbackred"]
	private val cardImage = scene2d.image(if (isFaceUp) faceUpDrawable else faceDownDrawable) {
		setPosition(0F, 0F, Align.center)
	}
	
	val cardGroup: CardGroup?
		get() = parent as? CardGroup?
	var isFaceUp: Boolean = isFaceUp
		set(value)
		{
			field = value
			cardImage.drawable = if (value) faceUpDrawable else faceDownDrawable
		}
	
	// Modules
	val smoothMovable = SmoothMovable(this, x, y, rotation)
	override val lockable: Lockable = object : Lockable(id, lockHolder, smoothMovable)
	{
		override fun longPress(): Boolean
		{
			if (Gdx.app.type == Application.ApplicationType.Android
				&& cardGroup != null && !draggable.justDragged && !rotatable.justRotated)
			{
				justLongPressed = true
				cardGroup!!.lockable.justLongPressed = true
				cardGroup!!.draggable.justDragged = true
				game.client?.apply {
					sendTCP(ObjectUnlockEvent(id, game.user.username))
					sendTCP(ObjectLockEvent(cardGroup!!.id, game.user.username))
				}
				return true
			}
			return false
		}
		
		override fun unlock()
		{
			if (isLockHolder && !draggable.justDragged && !rotatable.justRotated && !justLongPressed)
			{
				if (ownable.isOwned)
				{
					this@Card.isFaceUp = !this@Card.isFaceUp
					room.tabletop.hand.sendUpdates()
				}
				else
					game.client?.sendTCP(CardFlipEvent(id))
			}
			cardGroup?.arrange()
			super.unlock()
		}
	}
	val draggable: Draggable = object : Draggable(room, smoothMovable, lockable)
	{
		override val canDrag: Boolean
			get() = cardGroup == null || !cardGroup!!.lockable.justLongPressed && !UIUtils.shift()
		
		override fun drag() = separateFromCardGroup()
	}
	val rotatable: Rotatable = object : Rotatable(smoothMovable, lockable, draggable)
	{
		override fun pinch() = separateFromCardGroup()
	}
	override val highlightable = object : Highlightable(smoothMovable, lockable)
	{
		override val shouldExpand: Boolean
			get() = super.shouldExpand && (cardGroup == null || !cardGroup!!.lockable.isLocked)
	}
	val ownable = Ownable(room, id, draggable)
	
	init
	{
		this += cardImage
		
		// Add modules
		this += smoothMovable
		this += lockable
		this += draggable
		this += rotatable
		this += highlightable
		this += ownable
	}
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (super.hit(x, y, touchable) != null)
			return this
		return null
	}
	
	private fun separateFromCardGroup()
	{
		val cardGroup = cardGroup ?: return
		if (cardGroup.cardHolder != null || cardGroup.children.size > 2)
		{
			game.client?.apply {
				outgoingPacketBuffer += CardGroupChangeEvent(gdxArrayOf(toServerCard()), -1, game.user.username)
			}
			cardGroup -= this@Card
		}
		else
		{
			game.client?.apply {
				outgoingPacketBuffer += CardGroupDismantleEvent(cardGroup.id)
			}
			cardGroup.dismantle()
		}
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean =
		gObject is Card || gObject is CardGroup
	
	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
			game.client?.apply {
				outgoingPacketBuffer += CardGroupCreateEvent(cards = gdxArrayOf(toServerCard(), gObject.toServerCard()))
			}
		else if (gObject is CardGroup)
		{
			val cards = gdxArrayOf(this)
			for (actor: Actor in gObject.children)
			{
				if (actor is Card)
					cards += actor
			}
			gObject.dismantle()
			game.client?.apply {
				outgoingPacketBuffer += CardGroupDismantleEvent(gObject.id)
				outgoingPacketBuffer += CardGroupCreateEvent(cards = cards.map { it.toServerCard() })
			}
		}
	}
	
	override fun toString(): String = "Card($rank$suit, id=$id, parentId=${cardGroup?.id})"
	
	fun toServerCard(): ServerCard = ServerCard(
		id,
		smoothMovable.xInterpolator.target,
		smoothMovable.yInterpolator.target,
		smoothMovable.rotationInterpolator.target,
		rank,
		suit,
		isFaceUp
	)
	
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
