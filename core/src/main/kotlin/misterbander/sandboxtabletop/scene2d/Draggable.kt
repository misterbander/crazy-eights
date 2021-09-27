package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import ktx.collections.getOrPut
import ktx.math.component1
import ktx.math.component2
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.net.objectMovedEventPool

class Draggable(
	private val clickListener: ClickListener,
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	var stageOffsetX = 0F
	var stageOffsetY = 0F
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
				stageOffsetX = event.stageX - parent.x
				stageOffsetY = event.stageY - parent.y
				justDragged = true
			}
			
			override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				if (!lockable.isLockHolder || parent.getModule<Rotatable>()?.isPinching == true)
					return
				// x, y, dragStartX and dragStartY are all positions in local coords
				// To calculate drag delta we just need to calculate x - dragStartX, y - dragStartY
				// and translate them back into stage coords
				val (dragStartX, dragStartY) = parent.stageToLocalCoordinates(
					tempVec.set(parent.x + stageOffsetX, parent.y + stageOffsetY)
				)
				val (newStageX, newStageY) = parent.localToStageCoordinates(tempVec.set(x - dragStartX, y - dragStartY))
				smoothMovable.setPositionAndTargetPosition(newStageX, newStageY)
				val roomScreen = screen as RoomScreen
				roomScreen.objectMovedEvents.getOrPut(lockable.id) { objectMovedEventPool.obtain() }.apply {
					id = lockable.id
					this.x = newStageX
					this.y = newStageY
					rotation = smoothMovable.rotationInterpolator.target
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
