package misterbander.sandboxtabletop

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.KtxInputListener
import misterbander.gframework.GScreen

abstract class SandboxTabletopScreen(game: SandboxTabletop) : GScreen<SandboxTabletop>(game)
{
	val click = game.assetStorage.get<Sound>("sounds/click.wav")
	
	val transitionCamera = OrthographicCamera().apply { setToOrtho(false) }
	val transitionViewport = ExtendViewport(1280F, 720F, transitionCamera)
	
	override val transition by lazy { SmoothWipeTransition(this) }
	
	protected val ignoreTouchDown = object : KtxInputListener()
	{
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
		{
			event.cancel()
			return false
		}
	}
}
