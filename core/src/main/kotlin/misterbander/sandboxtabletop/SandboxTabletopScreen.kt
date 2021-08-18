package misterbander.sandboxtabletop

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.KtxInputListener
import misterbander.gframework.GScreen

abstract class SandboxTabletopScreen(game: SandboxTabletop) : GScreen<SandboxTabletop>(game)
{
	val transitionCamera = OrthographicCamera().apply { setToOrtho(false) }
	val transitionViewport = ExtendViewport(1280F, 720F, transitionCamera)
	val transition by lazy { Transition(this) }
	
	val click: Sound = game.assetManager["sounds/click.wav"]
	
	val ignoreTouchDown = object : KtxInputListener()
	{
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
		{
			event.cancel()
			return false
		}
	}
	
	override fun resize(width: Int, height: Int)
	{
		super.resize(width, height)
		transitionViewport.update(width, height, true)
	}
	
	override fun render(delta: Float)
	{
		transitionCamera.update()
		transition.update(delta)
		super.render(delta)
		transition.render()
	}
}
