package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import ktx.math.component1
import ktx.math.component2
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.net.objectMovedEventPool

class Draggable(
	private val lockable: Lockable,
	private val clickListener: ClickListener,
	private val smoothMovable: SmoothMovable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	init
	{
		parent.addListener(object : DragListener()
		{
			var offsetX = 0F
			var offsetY = 0F
			
			init
			{
				tapSquareSize = if (Gdx.app.type == Application.ApplicationType.Desktop) 1F else 5F
			}
			
			override fun dragStart(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				offsetX = event.stageX - parent.x
				offsetY = event.stageY - parent.y
			}
			
			override fun drag(event: InputEvent, x: Float, y: Float, pointer: Int)
			{
				if (!lockable.isLockHolder)
					return
				val (dragStartX, dragStartY) = parent.stageToLocalCoordinates(
					tempVec.set(
						parent.x + offsetX,
						parent.y + offsetY
					)
				)
				val (newStageX, newStageY) = parent.localToStageCoordinates(tempVec.set(x - dragStartX, y - dragStartY))
				smoothMovable.setPositionAndTargetPosition(newStageX, newStageY)
				(parent.screen as RoomScreen).objectMovedEvent = objectMovedEventPool.obtain().apply {
					id = lockable.id
					this.x = newStageX
					this.y = newStageY
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
