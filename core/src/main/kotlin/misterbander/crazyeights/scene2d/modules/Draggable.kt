package misterbander.crazyeights.scene2d.modules

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import ktx.app.Platform
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.net.objectMoveEventPool
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.scene2d.DragTarget
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec

open class Draggable(
	private val room: Room,
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable
) : GModule<CrazyEights>(smoothMovable.parent)
{
	val unrotatedDragPositionVec = vec2()
	val dragPositionVec = vec2()
	var justDragged = false
	private var currentDragTarget: DragTarget? = null
	
	init
	{
		parent.addListener(object : DragListener()
		{
			init
			{
				if (Platform.isDesktop)
					tapSquareSize = 8F
			}
			
			override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				// x, y are positions in local coords. We store the x and y in unrotatedDragPositionVec as if the
				// parent is not rotated when the user starts dragging
				unrotatedDragPositionVec.set(x, y).rotateDeg(parent.rotation)
				justDragged = canDrag
			}
			
			override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				if (!lockable.isLockHolder || parent.getModule<Rotatable>()?.isPinching == true || !justDragged)
					return
				drag()
				val ownable = parent.getModule<Ownable>()
				ownable?.updateOwnership(x, y)
				
				// To implement drag, we just need to move the object such that the mouse is always at
				// (dragPositionVec.x, dragPositionVec.y) in local coordinates
				// dragPositionVec is unrotatedDragPositionVec with rotation applied to account for rotation
				// interpolation
				dragPositionVec.set(unrotatedDragPositionVec)
				dragPositionVec.rotateDeg(-parent.rotation)
				val (newX, newY) = parent.localToParentCoordinates(tempVec.set(x, y).sub(dragPositionVec))
				smoothMovable.setPositionAndTargetPosition(newX, newY)
				ownable?.hand?.arrange() ?: game.client?.apply {
					val objectMoveEvent = removeFromOutgoingPacketBuffer<ObjectMoveEvent> { it.id == lockable.id }
						?: objectMoveEventPool.obtain()!!
					objectMoveEvent.apply {
						id = lockable.id
						this.x = newX
						this.y = newY
					}
					outgoingPacketBuffer += objectMoveEvent
				}
				
				updateDragTarget(event.stageX, event.stageY)
			}
			
			override fun dragStop(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				currentDragTarget?.apply {
					accept(parent)
					highlightable?.forceHighlight = false
				}
				currentDragTarget = null
				
				parent.getModule<Ownable>()?.apply {
					if (!isOwned && wasInHand)
					{
						wasInHand = false
						room.tabletop.hand.sendUpdates()
					}
				}
			}
		})
	}
	
	open fun drag() = Unit
	
	open val canDrag: Boolean
		get() = true
	
	fun updateDragTarget(x: Float, y: Float)
	{
		currentDragTarget?.highlightable?.forceHighlight = false
		currentDragTarget = null
		val dragTarget = room.tabletop.hitDragTarget(x, y)
		if (parent.getModule<Ownable>()?.isOwned == true)
			currentDragTarget = room.tabletop.hand
		else if (dragTarget?.canAccept(parent) == true)
		{
			currentDragTarget = dragTarget
			dragTarget.highlightable?.forceHighlight = true
		}
	}
	
	fun cancel()
	{
		justDragged = false
	}
}
