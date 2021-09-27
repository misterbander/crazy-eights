package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.IntSet
import ktx.actors.KtxInputListener
import ktx.collections.getOrPut
import ktx.math.component1
import ktx.math.component2
import ktx.math.minusAssign
import ktx.math.vec2
import misterbander.gframework.scene2d.GActorGestureListener
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.net.objectMovedEventPool

class Rotatable(
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable,
	draggable: Draggable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	private val roomScreen = screen as RoomScreen
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
			private val pointers = IntSet()
			private val stagePointer1 = vec2()
			private val stagePointer2 = vec2()
			
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				initialRotation = parent.rotation
				if (!isPinching)
				{
					// ActorGestureListener does not identify pointers that initially touched down
					// on this actor, we need to take care of that manually
					pointers.add(pointer)
					if (pointers.size == 2)
					{
						isPinching = true
						justStartedPinching = true
					}
				}
//				println("$parent pointer $pointer touch down, pointers = $pointers")
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				if (pointers.remove(pointer))
				{
					isPinching = false
					justStartedPinching = false
					if (pointers.size == 1)
					{
						var otherStageX = 0F
						var otherStageY = 0F
						if (MathUtils.isEqual(event.stageX, stagePointer1.x) && MathUtils.isEqual(event.stageY, stagePointer1.y))
						{
							otherStageX = stagePointer2.x
							otherStageY = stagePointer2.y
						}
						else if (MathUtils.isEqual(event.stageX, stagePointer2.x) && MathUtils.isEqual(event.stageY, stagePointer2.y))
						{
							otherStageX = stagePointer1.x
							otherStageY = stagePointer1.y
						}
//						println("pointer1=$stagePointer1")
//						println("pointer2=$stagePointer2")
//						println("x=${event.stageX} y=${event.stageY}")
						draggable.stageOffsetX = otherStageX - parent.x
						draggable.stageOffsetY = otherStageY - parent.y
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
				parent.localToStageCoordinates(stagePointer1.set(pointer1))
				parent.localToStageCoordinates(stagePointer2.set(pointer2))
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
				val (newStageX, newStageY) = parent.localToStageCoordinates(
					tempVec.set(pointerCenterX - centerOffsetVec.x, pointerCenterY - centerOffsetVec.y)
				)
				
				// Apply final position and rotation
				smoothMovable.setPositionAndTargetPosition(newStageX, newStageY)
				setRotation(initialRotation + dAngle, isImmediate = true)
				roomScreen.objectMovedEvents[lockable.id]!!.apply {
					x = newStageX
					y = newStageY
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
		roomScreen.objectMovedEvents.getOrPut(lockable.id) { objectMovedEventPool.obtain() }.apply {
			id = lockable.id
			this.x = smoothMovable.xInterpolator.target
			this.y = smoothMovable.yInterpolator.target
			this.rotation = smoothMovable.rotationInterpolator.target
		}
	}
}
