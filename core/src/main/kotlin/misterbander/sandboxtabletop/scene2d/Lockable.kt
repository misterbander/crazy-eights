package misterbander.sandboxtabletop.scene2d

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
	private var lockHolder: User? = null,
	smoothMovable: SmoothMovable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	init
	{
		parent.addListener(object : ActorGestureListener()
		{
			private var pointers = 0
			
			override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				pointers++
				if (!isLocked)
					game.client?.sendTCP(ObjectLockEvent(id, game.user.username))
			}
			
			override fun longPress(actor: Actor, x: Float, y: Float): Boolean
			{
				println("I AM LONG PRESSING OMGGGGGGG actor=$actor")
				return false
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				pointers--
				if (pointers == 0)
					game.client?.sendTCP(ObjectUnlockEvent(id, game.user.username))
			}
		})
	}
	
	val isLocked: Boolean
		get() = lockHolder != null
	
	val isLockHolder: Boolean
		get() = lockHolder == game.user
	
	open fun lock(user: User)
	{
		if (!isLocked)
		{
			parent.toFront()
			parent.setScrollFocus()
			lockHolder = user
		}
	}
	
	open fun unlock()
	{
		lockHolder = null
		parent.getModule<Draggable>()?.justDragged = false
		parent.getModule<Rotatable>()?.justRotated = false
	}
}
