package misterbander.sandboxtabletop.scene2d.modules

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import misterbander.gframework.scene2d.module.GModule
import misterbander.sandboxtabletop.SandboxTabletop

open class Highlightable(
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable
) : GModule<SandboxTabletop>(smoothMovable.parent)
{
	private val clickListener: ClickListener
	private var over = false
	var forceHighlight = false
	
	open val shouldHighlight: Boolean
		get() = over || forceHighlight
	open val shouldExpand: Boolean
		get() = !lockable.isLocked && clickListener.isPressed || lockable.isLocked
	
	init
	{
		clickListener = object : ClickListener()
		{
			override fun enter(event: InputEvent, x: Float, y: Float, pointer: Int, fromActor: Actor?)
			{
				if (pointer == -1)
					over = true
			}
			
			override fun exit(event: InputEvent, x: Float, y: Float, pointer: Int, toActor: Actor?)
			{
				if (pointer == -1)
					over = false
			}
		}
		parent.addListener(clickListener)
	}
	
	fun cancel()
	{
		clickListener.cancel()
		over = false
	}
	
	override fun update(delta: Float)
	{
		smoothMovable.scaleInterpolator.target = if (shouldExpand) 1.05F else 1F
	}
}
