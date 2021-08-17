package misterbander.gframework.scene2d

import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.utils.OrderedMap
import ktx.collections.plusAssign
import ktx.collections.set
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
	val modules = OrderedMap<Class<out GModule<T>>, GModule<T>>()
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
		modules.forEach { it.value.update(delta) }
	}
	
	/**
	 * Returns whether this `GObject` contains a module of the specified class.
	 * @param U concrete class of the module
	 * @return True if this `GObject` contains a module of the specified class, false otherwise.
	 */
	inline fun <reified U : GModule<T>> hasModule(): Boolean = getModule<U>() != null
	
	/**
	 * Returns the module of the specified class.
	 * @param U concrete class of the module
	 * @return The module of the specified class. If the module of the specified class does not exist, then null is
	 * returned.
	 */
	inline fun <reified U : GModule<T>> getModule(): U? = modules[U::class.java] as U?
	
	/**
	 * Adds a module of the specified class to this `GObject`.
	 * @param U concrete class of the module
	 */
	inline operator fun <reified U : GModule<T>> GObject<T>.plusAssign(module: U)
	{
		modules[U::class.java] = module
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
