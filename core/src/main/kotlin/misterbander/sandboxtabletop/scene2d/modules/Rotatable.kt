package misterbander.sandboxtabletop.scene2d.modules

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import ktx.actors.KtxInputListener
import ktx.collections.plusAssign
import ktx.math.component1
import ktx.math.component2
import ktx.math.minusAssign
import ktx.math.vec2
import misterbander.gframework.scene2d.GActorGestureListener
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.net.objectMovedEventPool
import misterbander.sandboxtabletop.net.objectRotatedEventPool
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectRotatedEvent

class Rotatable(
	private val room: Room,
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable,
	draggable: Draggable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	var justRotated = false
	var isPinching = false
	
	init
	{
		// Rotate using scroll wheel
		parent.addListener(object : KtxInputListener()
		{
			override fun scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean
			{
				if (!lockable.isLockHolder)
					return false
				setRotation(-amountY*15, true)
				return true
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				parent.stage.scrollFocus = null
			}
		})
		// Rotate using pinch gesture
		parent.addListener(object : GActorGestureListener()
		{
			private var justStartedPinching = false
			private var initialRotation = 0F
			private val initialDistanceVec = vec2()
			private val distanceVec = vec2()
			private var localCenterOffsetX = 0F
			private var localCenterOffsetY = 0F
			private val centerOffsetVec = vec2()
			private var pointer1 = -1
			private var pointer2 = -1
			private val pointer1Position = vec2()
			private val pointer2Position = vec2()
			
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				initialRotation = parent.rotation
				if (!isPinching)
				{
					// ActorGestureListener does not identify pointers that initially touched down
					// on this actor, we need to take care of that manually
					if (pointer1 == -1)
						pointer1 = pointer
					else if (pointer2 == -1)
						pointer2 = pointer
					if (pointer1 != -1 && pointer2 != -1)
					{
						isPinching = true
						justStartedPinching = true
					}
				}
//				println("$parent pointer $pointer touch down, pointers = $pointers")
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				if (pointer == pointer1 || pointer == pointer2)
				{
					var otherPointer = -1
					if (pointer == pointer1)
					{
						pointer1 = -1
						otherPointer = pointer2
					}
					else if (pointer == pointer2)
					{
						pointer2 = -1
						otherPointer = pointer1
					}
					isPinching = false
					justStartedPinching = false
					if (otherPointer != -1)
					{
						val otherX: Float
						val otherY: Float
						if (otherPointer == pointer1)
						{
							otherX = pointer1Position.x
							otherY = pointer1Position.y
						}
						else
						{
							otherX = pointer2Position.x
							otherY = pointer2Position.y
						}
//						println("pointer1=$stagePointer1")
//						println("pointer2=$stagePointer2")
//						println("x=${event.stageX} y=${event.stageY}")
						val (dragX, dragY) = parent.parentToLocalCoordinates(tempVec.set(otherX, otherY))
						draggable.unrotatedDragPositionVec.set(dragX, dragY).rotateDeg(parent.rotation)
					}
				}
//				println("$parent pointer $pointer touch up, pointers = $pointers")
			}
			
			override fun pinch(
				event: InputEvent,
				initialPointer1: Vector2,
				initialPointer2: Vector2,
				pointer1: Vector2,
				pointer2: Vector2
			)
			{
				if (!lockable.isLockHolder || !isPinching)
					return
				parent.localToParentCoordinates(pointer1Position.set(pointer1))
				parent.localToParentCoordinates(pointer2Position.set(pointer2))
				if (justStartedPinching)
				{
					localCenterOffsetX = (initialPointer1.x + initialPointer2.x)/2
					localCenterOffsetY = (initialPointer1.y + initialPointer2.y)/2
					justStartedPinching = false
				}
				
				// Calculate relative rotation angle
				initialDistanceVec.set(initialPointer2) -= initialPointer1
				distanceVec.set(pointer2) -= pointer1
				var dAngle = distanceVec.angleDeg(initialDistanceVec)
				if (dAngle > 180)
					dAngle -= 360
				
				// Calculate final center position in stage coordinates
				val pointerCenterX = (pointer1.x + pointer2.x)/2
				val pointerCenterY = (pointer1.y + pointer2.y)/2
				centerOffsetVec.set(localCenterOffsetX, localCenterOffsetY).rotateDeg(dAngle)
				val (newX, newY) = parent.localToParentCoordinates(
					tempVec.set(pointerCenterX - centerOffsetVec.x, pointerCenterY - centerOffsetVec.y)
				)
				
				// Apply final position and rotation
				smoothMovable.setPositionAndTargetPosition(newX, newY)
				setRotation(initialRotation + dAngle, isImmediate = true)
				val objectMovedEvent = room.findAndRemoveFromEventBuffer<ObjectMovedEvent> { it.id == lockable.id }
				(objectMovedEvent ?: objectMovedEventPool.obtain()!!).apply {
					id = lockable.id
					x = newX
					y = newY
					moverUsername = game.user.username
					room.eventBuffer += this
				}
			}
		})
	}
	
	private fun setRotation(rotation: Float, isRelative: Boolean = false, isImmediate: Boolean = false)
	{
		val newRotation = rotation + if (isRelative) smoothMovable.rotationInterpolator.target else 0F
		if (isImmediate)
			smoothMovable.rotationInterpolator.set(newRotation)
		else
			smoothMovable.rotationInterpolator.target = newRotation
		justRotated = true
		val objectRotatedEvent = room.findAndRemoveFromEventBuffer<ObjectRotatedEvent> { it.id == lockable.id }
		(objectRotatedEvent ?: objectRotatedEventPool.obtain()!!).apply {
			id = lockable.id
			this.rotation = smoothMovable.rotationInterpolator.target
			rotatorUsername = game.user.username
			room.eventBuffer += this
		}
	}
}
