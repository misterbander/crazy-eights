package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener
import misterbander.gframework.scene2d.module.GModule
import misterbander.sandboxtabletop.SandboxTabletop
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent

class Lockable(
	val id: Int,
	var lockHolder: User? = null,
	smoothMovable: SmoothMovable,
	onUnlock: (() -> Unit)? = null
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
					Network.client?.sendTCP(ObjectLockEvent(id, game.user.username))
			}
			
			override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int)
			{
				pointers--
				if (pointers == 0 && isLockHolder)
				{
					Network.client?.sendTCP(ObjectUnlockEvent(id))
					onUnlock?.invoke()
				}
			}
		})
	}
	
	val isLocked: Boolean
		get() = lockHolder != null
	
	val isLockHolder: Boolean
		get() = lockHolder == game.user
}
