package misterbander.crazyeights

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.actors.KtxInputListener
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.DefaultGScreen

abstract class CrazyEightsScreen(game: CrazyEights) : DefaultGScreen<CrazyEights>(game)
{
	val notoSansScSmall: BitmapFont = Scene2DSkin.defaultSkin["noto_sans_sc_small"]
	
	val click = game.assetStorage[Sounds.click]
	
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
