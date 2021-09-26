package misterbander.gframework.layer

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.Viewport
import misterbander.gframework.GFramework
import space.earlygrey.shapedrawer.ShapeDrawer

/**
 * A [GLayer] encapsulating a stage. On every frame, it first calls [Stage.act] in [update]. Then it sets the projection
 * matrices of the game's [Batch], [ShapeRenderer], [ShapeDrawer] before drawing the stage in [render]. And finally,
 * [Camera.update] is called in [postRender].
 * @param game parent [GFramework] instance
 * @param camera camera for the stage
 * @param viewport viewport for the stage
 * @param centerOnResize if true, then the camera will be recentered on screen resize
 */
open class StageLayer(
	private val game: GFramework,
	private val camera: Camera,
	private val viewport: Viewport,
	private val centerOnResize: Boolean
) : GLayer
{
	val stage = Stage(viewport, game.batch).apply {
		// For some reason stage updates the viewport using the screen's width and height and centers the camera
		// upon instantiation, which changes the camera position. So, if using ExtendViewport, whenever the window
		// size and the minimum world size do not match, this will cause world objects to appear aligned to the
		// left or to the top if screen size is not considered during world initiation.
		// We do not want that behavior for stage.
		// Hence, we need to undo the camera centering.
		if (!centerOnResize && viewport is ExtendViewport)
		{
			val extendViewport = viewport as ExtendViewport
			camera.position.set(extendViewport.minWorldWidth/2, extendViewport.minWorldHeight/2, 0F)
		}
	}
	
	override fun update(delta: Float) = stage.act(delta)
	
	override fun render(delta: Float)
	{
		game.batch.projectionMatrix = camera.combined
		game.shapeRenderer.projectionMatrix = camera.combined
		game.shapeDrawer.update()
		stage.draw()
	}
	
	override fun postRender(delta: Float) = camera.update()
	
	override fun resize(width: Int, height: Int) = stage.viewport.update(width, height, centerOnResize)
	
	override fun dispose() = stage.dispose()
}
