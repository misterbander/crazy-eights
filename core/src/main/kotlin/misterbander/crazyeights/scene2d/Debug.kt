package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.ObjectMap
import ktx.actors.txt
import ktx.collections.*
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.Room
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.gframework.scene2d.GObject
import com.badlogic.gdx.utils.StringBuilder as GdxStringBuilder

class Debug(private val room: Room) : GObject<CrazyEights>(room)
{
	private val lockedObjects = GdxArray<GObject<*>>()
	
	init
	{
		isVisible = false
	}
	
	override fun update(delta: Float)
	{
		if (Gdx.input.isKeyJustPressed(Input.Keys.F3))
			isVisible = !isVisible
		if (!isVisible)
			return
		
		val serverObjects = game.server?.state?.serverObjects
		val serverObjectsStr = serverObjects?.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" } ?: ""
		val serverObjectsDebug = "Server objects (${serverObjects?.size ?: 0}):\n$serverObjectsStr"
		val hands = game.server?.state?.hands
		val handsStr = hands?.joinToString(separator = "\n") { (key, value) -> "$key: ${value?.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { "    $it" }}" } ?: ""
		val handDebug = "Hands:\n$handsStr"
		
		// View all objects locked by the user
		lockedObjects.clear()
		for (actor: Actor in room.tabletop.cards.children)
		{
			if ((actor as? GObject<*>)?.getModule<Lockable>()?.isLockHolder == true)
				lockedObjects += actor
		}
		val lockedObjectsDebug = "Locked objects (${lockedObjects.size}):\n$lockedObjects"
		room.debugInfo.txt = "Scroll focus = ${room.stage.scrollFocus}\n$serverObjectsDebug\n$handDebug\n$lockedObjectsDebug"
	}
	
	private fun <T> GdxArray<T>.joinToString(
		separator: CharSequence = ", ",
		prefix: CharSequence = "",
		postfix: CharSequence = "",
		limit: Int = -1,
		truncated: CharSequence = "...",
		transform: ((T) -> CharSequence)? = null
	): String
	{
		val buffer = GdxStringBuilder(prefix)
		var count = 0
		for (i in 0 until size)
		{
			val element: T = this[i]
			if (++count > 1)
				buffer.append(separator)
			if (limit < 0 || count <= limit)
			{
				if (transform != null)
					buffer.append(transform(element))
				else
					buffer.append(element)
			}
			else
				break
		}
		if (limit in 0 until count)
			buffer.append(truncated)
		buffer.append(postfix)
		return buffer.toString()
	}
	
	private fun <K, V> GdxMap<K, V>.joinToString(
		separator: CharSequence = ", ",
		prefix: CharSequence = "",
		postfix: CharSequence = "",
		limit: Int = -1,
		truncated: CharSequence = "...",
		transform: ((ObjectMap.Entry<K, V>) -> CharSequence)? = null
	): String = toGdxArray().joinToString(separator, prefix, postfix, limit, truncated, transform)
	
	override fun setVisible(visible: Boolean)
	{
		super.setVisible(visible)
		room.debugInfo.isVisible = visible
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val shapeDrawer = game.shapeDrawer
		shapeDrawer.rectangle(2F, 2F, (1280 - 4).toFloat(), (720 - 4).toFloat(), Color.GREEN)
	}
}
