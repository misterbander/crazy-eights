package misterbander.sandboxtabletop

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Interpolation
import ktx.graphics.use

class Transition(private val screen: SandboxTabletopScreen)
{
	private var isRunning = false
	private var isTransitioningIn = false
	private var time = 0F
	private var duration = 0.4F
	private val progress: Float
		get() = time/duration
	private var targetScreen: SandboxTabletopScreen? = null
	
	fun start(isTransitioningIn: Boolean = false, targetScreen: SandboxTabletopScreen? = null)
	{
		screen.stage.addCaptureListener(screen.ignoreTouchDown)
		screen.uiStage.addCaptureListener(screen.ignoreTouchDown)
		isRunning = true
		this.isTransitioningIn = isTransitioningIn
		time = 0F
		this.targetScreen = targetScreen
	}
	
	fun update(delta: Float)
	{
		if (isRunning)
		{
			time += delta
			if (time > duration)
			{
				screen.stage.removeCaptureListener(screen.ignoreTouchDown)
				screen.uiStage.removeCaptureListener(screen.ignoreTouchDown)
				time = duration
				isRunning = false
				
				if (targetScreen != null)
				{
					screen.game.setScreen(targetScreen!!::class.java)
					targetScreen!!.transition.start(true)
				}
			}
		}
		
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
	
	fun render()
	{
		val shapeDrawer = screen.game.shapeDrawer
		val width = screen.transitionViewport.worldWidth
		val height = screen.transitionViewport.worldHeight
		if (isTransitioningIn)
		{
			screen.game.batch.use(screen.transitionCamera) {
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
			screen.game.batch.use(screen.transitionCamera) {
				shapeDrawer.update()
				shapeDrawer.filledRectangle((2*progress - 2)*width, 0F, width, height, Color.BLACK)
				shapeDrawer.filledRectangle(
					(2*progress - 1)*width, 0F, width, height,
					TRANSPARENT_COLOR, Color.BLACK, Color.BLACK, TRANSPARENT_COLOR
				)
			}
		}
	}
}
