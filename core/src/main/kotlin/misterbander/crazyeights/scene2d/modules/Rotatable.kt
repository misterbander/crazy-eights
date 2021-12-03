package misterbander.crazyeights.scene2d.modules

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import ktx.actors.KtxInputListener
import ktx.collections.*
import ktx.math.component1
import ktx.math.component2
import ktx.math.minusAssign
import ktx.math.vec2
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.net.objectMoveEventPool
import misterbander.crazyeights.net.objectRotateEventPool
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.gframework.scene2d.GActorGestureListener
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec

open class Rotatable(
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable,
	draggable: Draggable
) : GModule<CrazyEights>(smoothMovable.parent)
{
	var initialRotation = 0F
	var justRotated = false
	var isPinching = false
	
	init
	{
		// Rotate using scroll wheel
		parent.addListener(object : KtxInputListener()
		{
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean = true
			
			override fun scrolled(event: InputEvent, x: Float, y: Float, amountX: Float, amountY: Float): Boolean
			{
				if (!lockable.isLockHolder)
					return false
				setRotation(-amountY*15, true)
				return true
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				if (lockable.isLockHolder)
					parent.stage.scrollFocus = null
			}
		})
		// Rotate using pinch gesture
		parent.addListener(object : GActorGestureListener()
		{
			private var justStartedPinching = false
			private val initialDistanceVec = vec2()
			private val distanceVec = vec2()
			private var localCenterOffsetVec = vec2()
			private val centerOffsetVec = vec2()
			private var pointer1 = -1
			private var pointer2 = -1
			private val pointer1Position = vec2()
			private val pointer2Position = vec2()
			
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
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
						val (otherX, otherY) = parent.screenToLocalCoordinates(tempVec.set(Gdx.input.getX(otherPointer).toFloat(), Gdx.input.getY(otherPointer).toFloat()))
						draggable.unrotatedDragPositionVec.set(otherX, otherY).rotateDeg(parent.rotation)
					}
				}
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
				if (justStartedPinching)
				{
					localCenterOffsetVec.set((initialPointer1.x + initialPointer2.x)/2, (initialPointer1.y + initialPointer2.y)/2)
					justStartedPinching = false
					initialRotation = parent.rotation
				}
				
				pinch()
				
				parent.localToParentCoordinates(pointer1Position.set(pointer1))
				parent.localToParentCoordinates(pointer2Position.set(pointer2))
				
				// Calculate relative rotation angle
				initialDistanceVec.set(initialPointer2) -= initialPointer1
				distanceVec.set(pointer2) -= pointer1
				var dAngle = distanceVec.angleDeg(initialDistanceVec)
				if (dAngle > 180)
					dAngle -= 360
				
				// Calculate final center position in stage coordinates
				val pointerCenterX = (pointer1.x + pointer2.x)/2
				val pointerCenterY = (pointer1.y + pointer2.y)/2
				centerOffsetVec.set(localCenterOffsetVec).rotateDeg(dAngle)
				val (newX, newY) = parent.localToParentCoordinates(tempVec.set(pointerCenterX - centerOffsetVec.x, pointerCenterY - centerOffsetVec.y))
				
				// Apply final position and rotation
				smoothMovable.setPositionAndTargetPosition(newX, newY)
				setRotation(initialRotation + dAngle, isImmediate = true)
				
				val ownable = parent.getModule<Ownable>()
				ownable?.hand?.arrange() ?: game.client?.apply {
					val objectMoveEvent = removeFromOutgoingPacketBuffer<ObjectMoveEvent> { it.id == lockable.id }
						?: objectMoveEventPool.obtain()!!
					objectMoveEvent.apply {
						id = lockable.id
						x = newX
						y = newY
					}
					outgoingPacketBuffer += objectMoveEvent
				}
				ownable?.updateOwnership(0F, 0F)
				
				draggable.updateDragTarget(newX, newY)
			}
		})
	}
	
	open fun pinch() = Unit
	
	private fun setRotation(rotation: Float, isRelative: Boolean = false, isImmediate: Boolean = false)
	{
		val newRotation = rotation + if (isRelative) smoothMovable.rotationInterpolator.target else 0F
		if (isImmediate)
			smoothMovable.rotationInterpolator.set(newRotation)
		else
			smoothMovable.rotationInterpolator.target = newRotation
		justRotated = true
		parent.getModule<Ownable>()?.hand?.arrange() ?: game.client?.apply {
			val objectRotateEvent = removeFromOutgoingPacketBuffer<ObjectRotateEvent> { it.id == lockable.id }
				?: objectRotateEventPool.obtain()!!
			objectRotateEvent.apply {
				id = lockable.id
				this.rotation = smoothMovable.rotationInterpolator.target
			}
			outgoingPacketBuffer += objectRotateEvent
		}
	}
}
