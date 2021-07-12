package misterbander.gframework.scene2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Group
import ktx.collections.GdxArray
import ktx.collections.plusAssign
import misterbander.gframework.GFramework
import misterbander.gframework.GScreen
import misterbander.gframework.scene2d.module.GModule

/**
 * `GObject`s are special `Scene2D` groups that can hold child actors and can contain modules that add
 * custom behavior.
 * @property screen the parent GScreen
 */
abstract class GObject<T : GFramework>(val screen: GScreen<T>) : Group()
{
	protected val modules = GdxArray<GModule<T>>()
	var body: Body? = null
	
	/**
	 * Called when this `GObject` is spawned to the world and added to the stage. You can set up your
	 * `Box2D` bodies and fixtures here.
	 */
	open fun onSpawn() {}
	
	/**
	 * "Update" method for the `GObject`. This gets called every frame. If overridden, make sure to call `super.act()`.
	 */
	override fun act(delta: Float)
	{
		super.act(delta)
		modules.forEach { it.update(delta) }
	}
	
	/**
	 * Marks this `GObject` to be destroyed. It will be removed at the end of world time step.
	 */
	fun destroy()
	{
		screen.scheduledRemovalGObjects += this
	}
	
	override fun remove(): Boolean
	{
		val removed = super.remove()
		if (body != null)
		{
			screen.world?.destroyBody(body)
			body = null
		}
		return removed
	}
}
