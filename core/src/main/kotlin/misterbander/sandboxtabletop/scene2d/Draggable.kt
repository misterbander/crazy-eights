package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import ktx.collections.getOrPut
import ktx.math.component1
import ktx.math.component2
import ktx.math.vec2
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.Room
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.net.objectMovedEventPool

class Draggable(
	private val room: Room,
	private val clickListener: ClickListener,
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	val unrotatedDragPositionVec = vec2()
	val dragPositionVec = vec2()
	var justDragged = false
	
	init
	{
		parent.addListener(object : DragListener()
		{
			init
			{
				tapSquareSize = 8F
			}
			
			override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				// x, y are positions in local coords. We store the x and y in unrotatedDragPositionVec as if the
				// parent is not rotated when the user starts dragging
				unrotatedDragPositionVec.set(x, y).rotateDeg(parent.rotation)
				justDragged = true
			}
			
			override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				if (!lockable.isLockHolder || parent.getModule<Rotatable>()?.isPinching == true)
					return
				// To implement drag, we just need to move the object such that the mouse is always at
				// (dragPositionVec.x, dragPositionVec.y) in local coordinates
				// dragPositionVec is unrotatedDragPositionVec with rotation applied to account for rotation
				// interpolation
				dragPositionVec.set(unrotatedDragPositionVec)
				dragPositionVec.rotateDeg(-parent.rotation)
				val (newX, newY) = parent.localToParentCoordinates(tempVec.set(x, y).sub(dragPositionVec))
//				println(dragPositionVec)
				smoothMovable.setPositionAndTargetPosition(newX, newY)
				room.objectMovedEventBuffer.getOrPut(lockable.id) { objectMovedEventPool.obtain() }.apply {
					seqNumber = room.newSeqNumber
					id = lockable.id
					this.x = newX
					this.y = newY
				}
			}
		})
	}
	
	override fun update(delta: Float)
	{
		smoothMovable.scaleInterpolator.target =
			if (!lockable.isLocked && clickListener.isPressed || lockable.isLocked) 1.05F else 1F
	}
}
