package misterbander.gframework.layer

import com.badlogic.gdx.utils.Disposable
import misterbander.gframework.GScreen

/**
 * [GScreen] components can be separated into different logical layers, such as a stage layer, a UI layer, a layer for
 * `Box2D` worlds, a layer for transitions etc. `GLayer` provides a convenient way of grouping components in a layer.
 *
 * Each `GLayer` has their own update methods that are called in [GScreen.render]. It is divided into three stages:
 * [update], [render] and [postRender]. All `GLayer`'s [update] methods will be called, followed by [render],
 * and then [postRender].
 *
 * `GLayer`s also have a [resize] and [dispose] method that is called whenever its corresponding [GScreen] method is
 * called.
 */
interface GLayer : Disposable
{
	/**
	 * Called once every frame. You should put non-rendering related code in here.
	 * @param delta the time in seconds since the last render
	 */
	fun update(delta: Float) = Unit
	
	/**
	 * Called once every frame, right after [update]. All rendering related code goes here.
	 * @param delta the time in seconds since the last render
	 */
	fun render(delta: Float) = Unit
	
	/**
	 * Called once every frame, right after [render]. Remaining code that needs to be called after [render] goes here.
	 * @param delta the time in seconds since the last render
	 */
	fun postRender(delta: Float) = Unit
	
	/**
	 * Called when [GScreen.resize] is called.
	 * @param width new screen size in pixels
	 * @param height new screen size in pixels
	 */
	fun resize(width: Int, height: Int) = Unit
	
	override fun dispose() = Unit
}
