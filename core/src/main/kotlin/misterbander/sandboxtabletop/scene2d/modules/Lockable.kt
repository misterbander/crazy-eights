package misterbander.sandboxtabletop.scene2d.modules

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener
import ktx.actors.setScrollFocus
import misterbander.gframework.scene2d.module.GModule
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent

open class Lockable(
	val id: Int,
	lockHolder: User? = null,
	private val smoothMovable: SmoothMovable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	var lockHolder: User? = lockHolder
		private set
	
	val isLocked: Boolean
		get() = lockHolder != null
	val isLockHolder: Boolean
		get() = lockHolder == game.user
	var justLongPressed = false
	
	init
	{
		parent.addListener(object : ActorGestureListener()
		{
			private var pointers = 0
			
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				pointers++
				if (canLock)
					game.client?.sendTCP(ObjectLockEvent(id, game.user.username))
			}
			
			override fun longPress(actor: Actor, x: Float, y: Float): Boolean = this@Lockable.longPress(actor, x, y)
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				pointers--
				if (pointers == 0)
					game.client?.sendTCP(ObjectUnlockEvent(id, game.user.username))
			}
		})
	}
	
	open fun longPress(actor: Actor, x: Float, y: Float): Boolean = false
	
	open val canLock: Boolean
		get() = !isLocked && parent.getModule<Draggable>()?.canDrag ?: true
	
	open fun lock(user: User)
	{
		smoothMovable.xInterpolator.smoothingFactor = 2.5F
		smoothMovable.yInterpolator.smoothingFactor = 2.5F
		lockHolder = user
		parent.toFront()
		if (isLockHolder)
			parent.setScrollFocus()
	}
	
	open fun unlock()
	{
		lockHolder = null
		justLongPressed = false
		parent.getModule<Draggable>()?.justDragged = false
		parent.getModule<Rotatable>()?.justRotated = false
	}
}
