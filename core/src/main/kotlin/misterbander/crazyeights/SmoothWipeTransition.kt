package misterbander.crazyeights

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import ktx.graphics.use
import misterbander.gframework.layer.TransitionLayer

class SmoothWipeTransition(
	private val crazyEightsScreen: CrazyEightsScreen
) : TransitionLayer<CrazyEights>(crazyEightsScreen)
{
	private val game: CrazyEights
		get() = screen.game
	
	override val duration: Float
		get() = 0.4F
	
	override fun update(delta: Float)
	{
		super.update(delta)
		if (isTransitioningIn)
		{
			screen.uiCamera.zoom = Interpolation.exp5Out.apply(1.1F, 1F, progress)
			(screen.camera as OrthographicCamera).zoom = Interpolation.exp5Out.apply(1.2F, 1F, progress)
		}
		else
		{
			screen.uiCamera.zoom = Interpolation.exp5In.apply(1F, 1.1F, progress)
			(screen.camera as OrthographicCamera).zoom = Interpolation.exp5In.apply(1F, 1.2F, progress)
		}
	}
	
	override fun resize(width: Int, height: Int) = crazyEightsScreen.transitionViewport.update(width, height, true)
	
	override fun render(delta: Float)
	{
		val shapeDrawer = game.shapeDrawer
		val width = crazyEightsScreen.transitionViewport.worldWidth
		val height = crazyEightsScreen.transitionViewport.worldHeight
		if (isTransitioningIn)
		{
			game.batch.use(crazyEightsScreen.transitionCamera) {
				shapeDrawer.update()
				shapeDrawer.filledRectangle(2*progress*width, 0F, width, height, Color.BLACK)
				shapeDrawer.filledRectangle(
					(2*progress - 1)*width, 0F, width, height,
					Color.BLACK, TRANSPARENT_COLOR, TRANSPARENT_COLOR, Color.BLACK
				)
			}
		}
		else
		{
			game.batch.use(crazyEightsScreen.transitionCamera) {
				shapeDrawer.update()
				shapeDrawer.filledRectangle((2*progress - 2)*width, 0F, width, height, Color.BLACK)
				shapeDrawer.filledRectangle(
					(2*progress - 1)*width, 0F, width, height,
					TRANSPARENT_COLOR, Color.BLACK, Color.BLACK, TRANSPARENT_COLOR
				)
			}
		}
	}
	
	override fun postRender(delta: Float) = crazyEightsScreen.transitionCamera.update()
}
