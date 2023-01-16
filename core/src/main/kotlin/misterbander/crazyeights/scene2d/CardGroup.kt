package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import ktx.actors.plusAssign
import ktx.actors.setScrollFocus
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.server.ServerCardGroup
import misterbander.crazyeights.net.server.User
import misterbander.crazyeights.scene2d.modules.Draggable
import misterbander.crazyeights.scene2d.modules.Highlightable
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.Ownable
import misterbander.crazyeights.scene2d.modules.Rotatable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.scene2d.GObject
import kotlin.math.round

class CardGroup(
	private val room: RoomScreen,
	val id: Int = -1,
	x: Float = 0F,
	y: Float = 0F,
	rotation: Float = 0F,
	var spreadSeparation: Float = 40F,
	var spreadCurvature: Float = 0.07F,
	val cards: GdxArray<Groupable<CardGroup>> = GdxArray(),
	var type: ServerCardGroup.Type = ServerCardGroup.Type.STACK,
	lockHolder: User? = null
) : Groupable<CardGroup>(room), DragTarget
{
	private val tabletop: Tabletop
		get() = room.tabletop
	
	val cardHolder: CardHolder?
		get() = parent as? CardHolder
	private val ghosts = GdxMap<Groupable<CardGroup>, CardGhost>()
	private val comparator = Comparator<Groupable<CardGroup>> { o1, o2 -> if (o1.x == o2.x) 0 else if (o1.x > o2.x) 1 else -1 }
	
	// Modules
	override val smoothMovable = SmoothMovable(this, x, y, rotation)
	override val lockable: Lockable = object : Lockable(this, id, lockHolder, smoothMovable)
	{
		override fun lock(user: User)
		{
			super.lock(user)
			if (isLockHolder)
				Gdx.input.vibrate(100)
		}
		
		override fun unlock(sideEffects: Boolean)
		{
			if (cardHolder != null)
				smoothMovable.rotation = 0F
			super.unlock(sideEffects)
		}
	}
	override val draggable: Draggable = object : Draggable(this, room, smoothMovable, lockable)
	{
		override val canDrag: Boolean
			get() = !ownable.isOwned && (UIUtils.shift() || lockable.justLongPressed) && !tabletop.isGameStarted
		
		override fun pan() = detachFromCardHolder()
	}
	override val rotatable: Rotatable = object : Rotatable(this, smoothMovable, lockable, draggable)
	{
		override fun pinch() = detachFromCardHolder()
	}
	override val highlightable = object : Highlightable(this)
	{
		override val shouldHighlight: Boolean
			get() = over && UIUtils.shift() && !ownable.isOwned && !tabletop.isGameStarted
				|| lockable.isLockHolder || forceHighlight
		
		override val shouldExpand: Boolean
			get() = lockable.isLocked
	}
	val ownable = Ownable(this, room, id)
	
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
	
	override fun update(delta: Float)
	{
		if (type != ServerCardGroup.Type.SPREAD)
		{
			ghosts.values().forEach { it.remove() }
			ghosts.clear()
			return
		}
		// Create ghosts when dragging cards
		var needsArrange = false
		for (groupable: Groupable<CardGroup> in cards)
		{
			if (groupable.draggable.justDragged || groupable.rotatable.justRotated || groupable.rotatable.isPinching)
			{
				if (groupable in ghosts)
					continue
				val ghost = CardGhost(groupable)
				ghosts[groupable] = ghost
				addActor(ghost)
				needsArrange = true
			}
			else
				ghosts.remove(groupable)?.remove()
		}
		if (needsArrange)
			arrange(false)
	}
	
	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		val hit: Actor? = super.hit(x, y, touchable)
		if (hit != null)
			return if (lockable.isLocked) this else if (type == ServerCardGroup.Type.SPREAD) hit else cards.peek()
		return null
	}
	
	operator fun plusAssign(groupable: Groupable<CardGroup>) = insert(groupable, cards.size)
	
	fun insert(groupable: Groupable<CardGroup>, index: Int)
	{
		groupable.transformToGroupCoordinates(this)
		cards.insert(index, groupable)
		addActor(groupable)
	}
	
	operator fun minusAssign(groupable: Groupable<CardGroup>)
	{
		groupable.transformToGroupCoordinates(tabletop.cards)
		highlightable.cancel()
		cardHolder?.highlightable?.cancel()
		cards -= groupable
		tabletop.cards += groupable
		ghosts.remove(groupable)?.remove()
	}
	
	private fun detachFromCardHolder()
	{
		val cardHolder = cardHolder ?: return
		transformToGroupCoordinates(tabletop.cards)
		game.client?.apply {
			outgoingPacketBuffer += CardGroupDetachEvent(cardHolder.id, changerUsername = game.user.name)
		}
		cardHolder.highlightable.cancel()
		tabletop.cards += this
		setScrollFocus()
		
		if (type == ServerCardGroup.Type.PILE)
		{
			type = ServerCardGroup.Type.STACK
			arrange()
		}
	}
	
	override fun canAccept(gObject: GObject<CrazyEights>): Boolean
	{
		if (tabletop.isGameStarted)
			return gObject is Card && this != tabletop.drawStack
		return gObject is Card || gObject is CardGroup
	}
	
	override fun accept(gObject: GObject<CrazyEights>)
	{
		if (gObject is Card)
			game.client?.apply {
				outgoingPacketBuffer += CardGroupChangeEvent(gdxArrayOf(gObject.toServerCard()), id, game.user.name)
			}
		else if (gObject is CardGroup)
		{
			val cards = GdxArray<Card>()
			for (groupable: Groupable<CardGroup> in gObject.cards)
			{
				if (groupable is Card)
					cards += groupable
			}
			gObject.dismantle()
			game.client?.apply {
				outgoingPacketBuffer += CardGroupDismantleEvent(gObject.id)
				outgoingPacketBuffer += CardGroupChangeEvent(cards.map { it.toServerCard() }, id, game.user.name)
			}
		}
	}
	
	fun arrange(sort: Boolean = true)
	{
		when (type)
		{
			ServerCardGroup.Type.STACK ->
			{
				cards.forEachIndexed { index, groupable: Groupable<CardGroup> ->
					groupable.smoothMovable.apply {
						xInterpolator.smoothingFactor = 5F
						yInterpolator.smoothingFactor = 5F
						setPosition(-index.toFloat(), index.toFloat())
						rotation = 180*round(rotation/180)
						groupable.zIndex = index
					}
				}
			}
			ServerCardGroup.Type.PILE ->
			{
				for (groupable: Groupable<CardGroup> in cards)
				{
					groupable.smoothMovable.apply {
						xInterpolator.smoothingFactor = 5F
						yInterpolator.smoothingFactor = 5F
					}
				}
			}
			ServerCardGroup.Type.SPREAD ->
			{
				if (sort)
					cards.sort(comparator)
				var ghostsEncountered = 0
				cards.forEachIndexed { index, groupable: Groupable<CardGroup> ->
					val (x, y) = getSpreadPositionForIndex(index, cards.size, spreadSeparation, spreadCurvature)
					val rotation = getSpreadRotationForIndex(index, cards.size, spreadSeparation, spreadCurvature)
					val ghost: CardGhost? = ghosts[groupable]
					if (!groupable.lockable.isLocked)
					{
						groupable.smoothMovable.apply {
							xInterpolator.smoothingFactor = 5F
							yInterpolator.smoothingFactor = 5F
							setPosition(x, y)
							this.rotation = rotation
						}
					}
					if (ghost == null)
						groupable.zIndex = index + ghostsEncountered
					else
					{
						groupable.zIndex = index + ghostsEncountered + 1
						ghost.setPosition(x, y)
						ghost.rotation = rotation
						ghost.zIndex = index + ghostsEncountered
						ghostsEncountered++
					}
				}
			}
		}
	}
	
	fun flip(isFaceUp: Boolean)
	{
		for (groupable: Groupable<CardGroup> in cards)
		{
			if (groupable is Card)
				groupable.isFaceUp = isFaceUp
			else if (groupable is CardGroup)
				groupable.flip(isFaceUp)
		}
	}
	
	fun dismantle()
	{
		while (cards.isNotEmpty())
			this -= cards.first()
		tabletop.idToGObjectMap.remove(id)
		remove()
	}
	
	override fun clearChildren()
	{
		super.clearChildren()
		cards.clear()
		ghosts.clear()
	}
	
	override fun toString(): String = "CardGroup(id=$id, cards=${cards.size})"
	
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
