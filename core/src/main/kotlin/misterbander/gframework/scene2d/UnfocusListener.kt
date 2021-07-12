package misterbander.gframework.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import ktx.actors.KtxInputListener

/**
 * Utility listener for windows and stages that unfocuses text fields, making it possible to
 * 'unselect' a text field.
 */
class UnfocusListener(private val actor: Actor) : KtxInputListener()
{
	override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
	{
		if (event.target !is MBTextField)
		{
			actor.stage?.keyboardFocus = null
			Gdx.input.setOnscreenKeyboardVisible(false)
		}
		return true
	}
}
