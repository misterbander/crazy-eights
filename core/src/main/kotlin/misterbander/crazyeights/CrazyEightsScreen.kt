package misterbander.crazyeights

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.KtxInputListener
import misterbander.gframework.GScreen

abstract class CrazyEightsScreen(game: CrazyEights) : GScreen<CrazyEights>(game)
{
	val click = game.assetStorage.get<Sound>("sounds/click.wav")
	
	val transition by lazy {
		SmoothWipeTransition(this, ExtendViewport(1280F, 720F, OrthographicCamera().apply { setToOrtho(false) }))
	}
	override val layers by lazy { arrayOf(mainLayer, uiLayer, transition) }
	
	protected val ignoreTouchDownListener = object : KtxInputListener()
	{
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
		{
			event.cancel()
			return false
		}
	}
}
