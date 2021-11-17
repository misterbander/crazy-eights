package misterbander.crazyeights.scene2d.modules

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import misterbander.crazyeights.CrazyEights
import misterbander.gframework.scene2d.module.GModule

open class Highlightable(
	private val smoothMovable: SmoothMovable,
	private val lockable: Lockable
) : GModule<CrazyEights>(smoothMovable.parent)
{
	private val clickListener: ClickListener
	private var over = false
	var forceHighlight = false
	
	open val shouldHighlight: Boolean
		get() = over || lockable.isLockHolder || forceHighlight
	open val shouldExpand: Boolean
		get() = clickListener.isPressed || lockable.isLocked
	
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
