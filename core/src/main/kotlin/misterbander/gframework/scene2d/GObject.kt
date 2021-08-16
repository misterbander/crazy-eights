package misterbander.gframework.scene2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Group
import ktx.collections.GdxArray
import ktx.collections.plusAssign
import misterbander.gframework.GFramework
import misterbander.gframework.GScreen
import misterbander.gframework.scene2d.module.GModule

/**
 * `GObject`s are special `Scene2D` groups that can hold child actors and can contain modules that add custom behavior.
 *
 * `GObject` defines [onSpawn], which gets called whenever the [GObject] is added to the world.
 *
 * `GObject` also defines [destroy], which can be used to safely remove [GObject]s with `Box2D` bodies during collision
 * callbacks.
 * @property screen the parent GScreen
 */
abstract class GObject<T : GFramework>(val screen: GScreen<T>) : Group()
{
	protected val modules = GdxArray<GModule<T>>()
	var body: Body? = null
	
	/**
	 * Called when this `GObject` is spawned in the world. You can set up your `Box2D` bodies and fixtures here.
	 */
	open fun onSpawn() = Unit
	
	/**
	 * "Update" method for the `GObject`. This gets called every frame. If overridden, make sure to call `super.act()`.
	 */
	override fun act(delta: Float)
	{
		super.act(delta)
		modules.forEach { it.update(delta) }
	}
	
	/**
	 * Marks this `GObject` for removal. This does not immediately remove the `GObject` from the world, it will be
	 * removed at the end of the next world time step. Useful during collision callbacks where creation and removal of
	 * objects are prohibited.
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
