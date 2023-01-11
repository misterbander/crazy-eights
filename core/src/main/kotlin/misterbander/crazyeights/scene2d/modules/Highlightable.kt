package misterbander.crazyeights.scene2d.modules

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.OpponentHand
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.module.GModule

open class Highlightable(private val parent: GObject<CrazyEights>) : GModule
{
	private val clickListener: ClickListener
	protected var over = false
		private set
	var forceHighlight = false
	
	open val shouldHighlight: Boolean
		get() = over || parent.getModule<Lockable>()?.isLockHolder == true || forceHighlight
	open val shouldExpand: Boolean
		get() = clickListener.isPressed || parent.getModule<Lockable>()?.isLocked == true
	
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
		parent.getModule<SmoothMovable>()?.scale = if (parent is CardGroup && parent.parent is OpponentHand)
			0.7F
		else if (shouldExpand)
			1.05F
		else
			1F
	}
}
