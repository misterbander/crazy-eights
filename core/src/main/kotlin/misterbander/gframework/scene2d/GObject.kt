package misterbander.gframework.scene2d

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.OrderedMap
import ktx.actors.isShown
import ktx.collections.*
import misterbander.gframework.GFramework
import misterbander.gframework.GScreen
import misterbander.gframework.scene2d.module.GModule

/**
 * `GObject`s are special `Scene2D` groups that can hold child actors and can contain modules that add custom behavior.
 *
 * Whenever the `GObject` is added to a stage, [onSpawn], as well as the `onSpawn` methods of all modules, will be called.
 *
 * `GObject` also defines [destroy], which can be used to delay removal. Useful for safely removing [GObject]s during
 * collision callbacks where creation and removal of objects are prohibited.
 * @property screen the parent [GScreen]
 */
abstract class GObject<T : GFramework>(val screen: GScreen<T>) : Group()
{
	val game: T
		get() = screen.game
	val modules = OrderedMap<Class<out GModule<*>>, GModule<*>>()
	private var isAlive = false
	
	/**
	 * Called when this `GObject` is added to a stage. This is called right after `onSpawn` methods of all modules have
	 * been called.
	 */
	open fun onSpawn() = Unit
	
	override fun act(delta: Float)
	{
		super.act(delta)
		for (module: GModule<*> in modules.values())
		{
			module.update(delta)
			for (i in 0 until screen.fixedUpdateCount)
				module.fixedUpdate()
		}
		update(delta)
		for (i in 0 until screen.fixedUpdateCount)
			fixedUpdate()
	}
	
	/**
	 * Called once every frame, right after `update` and `fixedUpdate` methods of all modules have been called.
	 * @param delta the time in seconds since the last render
	 */
	open fun update(delta: Float) = Unit
	
	/**
	 * Similar to [update], but this is called in a fixed timestep fashion, i.e. delta is always fixed at 1/60F, hence
	 * why there is no delta parameter passed. This is called right after `update` and `fixedUpdate` methods of all
	 * modules have been called.
	 *
	 * It is guaranteed that `fixedUpdate` is called 60 times per second. However, in case of an FPS drop, `fixedUpdate`
	 * might be called more than once per frame as means of 'catching up'.
	 *
	 * Physics related code should be placed here.
	 */
	open fun fixedUpdate() = Unit
	
	/**
	 * Returns whether this `GObject` contains a module of the specified class.
	 * @param U concrete class of the module
	 * @return True if this `GObject` contains a module of the specified class, false otherwise.
	 */
	inline fun <reified U : GModule<*>> hasModule(): Boolean = getModule<U>() != null
	
	/**
	 * Returns the module of the specified class.
	 * @param U concrete class of the module
	 * @return The module of the specified class. If the module of the specified class does not exist, then null is
	 * returned.
	 */
	inline fun <reified U : GModule<*>> getModule(): U? = modules[U::class.java] as U?
	
	/**
	 * Adds a module of the specified class to this `GObject`.
	 * @param U concrete class of the module
	 */
	inline operator fun <reified U : GModule<*>> GObject<*>.plusAssign(module: U)
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
		if (removed)
		{
			modules.values().forEach { it.onDestroy() }
			isAlive = false
		}
		return removed
	}
	
	override fun setStage(stage: Stage?)
	{
		super.setStage(stage)
		if (!isAlive && isShown())
		{
			modules.values().forEach { it.onSpawn() }
			onSpawn()
			isAlive = true
		}
	}
}
