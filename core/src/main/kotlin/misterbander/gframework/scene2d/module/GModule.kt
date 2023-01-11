package misterbander.gframework.scene2d.module

import misterbander.gframework.scene2d.GObject

/**
 * Modules can be added to [GObject]s to add custom behavior.
 */
interface GModule
{
	/**
	 * Called once every frame.
	 * @param delta the time in seconds since the last render
	 */
	fun update(delta: Float) = Unit
	
	/**
	 * Similar to [update], but this is called in a fixed timestep fashion, i.e. delta is always fixed at 1/60F, hence
	 * why there is no delta parameter passed.
	 *
	 * It is guaranteed that `fixedUpdate` is called 60 times per second. However, in case of an FPS drop, `fixedUpdate`
	 * might be called more than once per frame as means of 'catching up'.
	 *
	 * Physics related code should be placed here.
	 */
	fun fixedUpdate() = Unit
	
	/**
	 * Called whenever the parent [GObject] is removed from a stage.
	 */
	fun onDestroy() = Unit
}
