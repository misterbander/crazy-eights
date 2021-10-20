package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import com.badlogic.gdx.utils.Align
import ktx.actors.plusAssign
import ktx.collections.GdxIntArray
import ktx.collections.plusAssign
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCard.Rank
import misterbander.crazyeights.model.ServerCard.Suit
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardGroupChangedEvent
import misterbander.crazyeights.net.packets.CardGroupCreatedEvent
import misterbander.crazyeights.net.packets.CardGroupDismantledEvent
import misterbander.crazyeights.net.packets.FlipCardEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.tempVec

class Card(
	private val room: Room,
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
	private val faceUpDrawable: Drawable = Scene2DSkin.defaultSkin[if (suit == Suit.JOKER) "cardjoker" else "card${suit}${rank}"]
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
		override fun longPress(actor: Actor, x: Float, y: Float): Boolean
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
				game.client?.sendTCP(FlipCardEvent(id))
			cardGroup?.arrange()
			super.unlock()
		}
	}
	val draggable: Draggable = object : Draggable(room, smoothMovable, lockable)
	{
		override val canDrag: Boolean
			get() = cardGroup == null || !UIUtils.shift()
		
		override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int)
		{
			if (cardGroup != null)
				separateFromCardGroup()
		}
	}
	val rotatable: Rotatable = object : Rotatable(smoothMovable, lockable, draggable)
	{
		override fun pinch(
			event: InputEvent,
			initialPointer1: Vector2,
			initialPointer2: Vector2,
			pointer1: Vector2,
			pointer2: Vector2
		)
		{
			if (cardGroup != null)
				separateFromCardGroup()
		}
	}
	override val highlightable = object : Highlightable(smoothMovable, lockable)
	{
		override val shouldExpand: Boolean
			get() = super.shouldExpand && (cardGroup == null || !cardGroup!!.lockable.isLocked)
	}
	
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
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (super.hit(x, y, touchable) != null)
			return this
		return null
	}
	
	private fun separateFromCardGroup()
	{
		val cardGroup = cardGroup!!
		if (cardGroup.children.size > 2)
		{
			cardGroup -= this@Card
			game.client?.apply { outgoingPacketBuffer += CardGroupChangedEvent(intArrayOf(id), -1, game.user.username) }
		}
		else
		{
			cardGroup.dismantle()
			game.client?.apply { outgoingPacketBuffer += CardGroupDismantledEvent(cardGroup.id, game.user.username) }
		}
	}
	
	fun transformToGroupCoordinates(group: Group)
	{
		val (newX1, newY1) = localToActorCoordinates(group, tempVec.set(0F, 0F))
		val (newX2, newY2) = localToActorCoordinates(group, tempVec.set(1F, 0F))
		val rotation = smoothMovable.rotationInterpolator.target
		val newRotation = MathUtils.atan2(newY2 - newY1, newX2 - newX1)*MathUtils.radDeg
		val deltaRotation = newRotation - rotation
		smoothMovable.setPositionAndTargetPosition(newX1, newY1)
		smoothMovable.rotationInterpolator.set(newRotation)
		draggable.unrotatedDragPositionVec.rotateDeg(deltaRotation)
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean = gObject is Card || gObject is CardGroup

	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
			game.client?.apply { outgoingPacketBuffer += CardGroupCreatedEvent(cardIds = intArrayOf(id, gObject.id)) }
		else if (gObject is CardGroup)
		{
			val cardIds = GdxIntArray().apply { add(id) }
			gObject.children.forEach {
				if (it is Card)
					cardIds.add(it.id)
			}
			gObject.dismantle()
			game.client?.apply {
				outgoingPacketBuffer += CardGroupDismantledEvent(gObject.id)
				outgoingPacketBuffer += CardGroupCreatedEvent(cardIds = cardIds.toArray())
			}
		}
	}
	
	override fun toString(): String = "Card(id=$id, rank=$rank, suit=$suit, parentId=${cardGroup?.id})"
	
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
