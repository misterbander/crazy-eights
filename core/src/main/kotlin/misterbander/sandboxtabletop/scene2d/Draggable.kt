package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.DragListener
import ktx.math.component1
import ktx.math.component2
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.tempVec
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.objectMovedEventPool
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent

class Draggable(
	private val id: Int,
	var lockHolder: User? = null,
	private val clickListener: ClickListener,
	private val smoothMovable: SmoothMovable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	init
	{
		parent.addListener(object : ActorGestureListener()
		{
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				if (!isLocked)
					Network.client?.sendTCP(ObjectLockEvent(id, parent.screen.game.user.username))
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				if (isLockHolder)
					Network.client?.sendTCP(ObjectUnlockEvent(id))
			}
		})
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
				if (!isLockHolder)
					return
				val (dragStartX, dragStartY) = parent.stageToLocalCoordinates(tempVec.set(parent.x + offsetX, parent.y + offsetY))
				val (newStageX, newStageY) = parent.localToStageCoordinates(tempVec.set(x - dragStartX, y - dragStartY))
				smoothMovable.setPositionAndTargetPosition(newStageX, newStageY)
				(parent.screen as RoomScreen).objectMovedEvent = objectMovedEventPool.obtain().apply {
					id = this@Draggable.id
					this.x = newStageX
					this.y = newStageY
				}
			}
		})
	}
	
	val isLocked: Boolean
		get() = lockHolder != null
	
	val isLockHolder: Boolean
		get() = lockHolder == parent.screen.game.user
	
	override fun update(delta: Float)
	{
		smoothMovable.scaleInterpolator.target = if (!isLocked && clickListener.isPressed || isLocked) 1.05F else 1F
	}
}
