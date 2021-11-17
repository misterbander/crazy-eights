package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import ktx.actors.plusAssign
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject

class CardGroup(
	private val room: Room,
	val id: Int,
	x: Float,
	y: Float,
	rotation: Float,
	cards: GdxArray<Card>,
	val type: ServerCardGroup.Type = ServerCardGroup.Type.STACK,
	lockHolder: User? = null
) : GObject<CrazyEights>(room), DragTarget
{
	val cardHolder: CardHolder?
		get() = parent as? CardHolder?
	
	// Modules
	private val smoothMovable = SmoothMovable(this, x, y, rotation)
	override val lockable: Lockable = object : Lockable(id, lockHolder, smoothMovable)
	{
		override fun lock(user: User)
		{
			super.lock(user)
			if (isLockHolder)
				Gdx.input.vibrate(100)
		}
	}
	val draggable: Draggable = object : Draggable(room, smoothMovable, lockable)
	{
		override val canDrag: Boolean
			get() = UIUtils.shift() || lockable.justLongPressed
		
		override fun drag() = detachFromCardHolder()
	}
	private val rotatable = Rotatable(smoothMovable, lockable, draggable)
	override val highlightable = object : Highlightable(smoothMovable, lockable)
	{
		override val shouldExpand: Boolean
			get() = lockable.isLocked
	}
	private val ownable = Ownable(room, id, draggable)
	
	init
	{
		cards.forEach { addActor(it) }
		arrange()
		
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
			return if (lockable.isLocked) this else children.peek()
		return null
	}
	
	operator fun plusAssign(card: Card)
	{
		card.transformToGroupCoordinates(this)
		addActor(card)
		arrange()
	}
	
	operator fun minusAssign(card: Card)
	{
		card.transformToGroupCoordinates(room.tabletop.cards)
		highlightable.cancel()
		cardHolder?.highlightable?.cancel()
		room.tabletop.cards += card
	}
	
	private fun detachFromCardHolder()
	{
		val cardHolder = cardHolder ?: return
		transformToGroupCoordinates(room.tabletop.cards)
		cardHolder.highlightable.cancel()
		game.client?.apply {
			outgoingPacketBuffer += CardGroupDetachEvent(cardHolder.id, changerUsername = game.user.username)
		}
		room.tabletop.cards += this
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean = gObject is Card || gObject is CardGroup
	
	@Suppress("UNCHECKED_CAST")
	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
			game.client?.apply { outgoingPacketBuffer += CardGroupChangeEvent(intArrayOf(gObject.id), id, game.user.username) }
		else if (gObject is CardGroup)
		{
			val cardIds = GdxIntArray()
			for (actor: Actor in gObject.children)
			{
				if (actor is Card)
					cardIds.add(actor.id)
			}
			gObject.dismantle()
			game.client?.apply {
				outgoingPacketBuffer += CardGroupDismantleEvent(gObject.id)
				outgoingPacketBuffer += CardGroupChangeEvent(cardIds.toArray(), id, game.user.username)
			}
		}
	}
	
	fun arrange()
	{
		if (type == ServerCardGroup.Type.STACK)
		{
			for (i in 0 until children.size)
			{
				(children[i] as Card).smoothMovable.apply {
					xInterpolator.smoothingFactor = 5F
					yInterpolator.smoothingFactor = 5F
					setTargetPosition(-(i - 1).toFloat(), (i - 1).toFloat())
					var oldTarget = rotationInterpolator.target.mod(360F)
					if (oldTarget > 180F)
						oldTarget -= 360F
					rotationInterpolator.target = if (oldTarget in -90F..90F) 0F else 180F
				}
			}
		}
	}
	
	fun dismantle()
	{
		while (children.isNotEmpty())
			this -= children.first() as Card
		remove()
	}
	
	override fun toString(): String = "CardGroup(id=$id, cards=${children.size})"
	
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
