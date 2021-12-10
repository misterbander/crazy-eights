package misterbander.gframework.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Action
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import ktx.actors.onKeyboardFocus
import ktx.math.vec2
import misterbander.gframework.GScreen

/**
 * Makes [Dialog]s that contain text fields accessible on mobile such that while editing them, the window gets shifted
 * upwards, so it doesn't get covered by the on-screen keyboard.
 *
 * For this to work, a keyboard height listener should be attached in the mobile platform that calls [onKeyboardHeightChanged].
 * [GScreen] has a convenient set to store [KeyboardHeightObserver]s so that they can be easily accessed from the mobile
 * platform.
 */
abstract class AccessibleInputDialog(
	title: String,
	skin: Skin,
	styleName: String
) : Dialog(title, skin, styleName), KeyboardHeightObserver
{
	private val prevWindowPos = vec2()
	private val windowScreenPos = vec2()
	private val textFieldScreenPos = vec2()
	private var keyboardHeight = 0
	private var shouldShift = false
	
	fun addFocusListener(gTextField: GTextField)
	{
		gTextField.onKeyboardFocus { focused -> if (focused) adjustPosition(keyboardHeight) }
	}
	
	override fun onKeyboardHeightChanged(height: Int, orientation: Int)
	{
		this.keyboardHeight = height
		
		if (height > 0) // Keyboard up
		{
			prevWindowPos.set(x, y)
			if (adjustPosition(height))
				shouldShift = true
		}
		else if (shouldShift)
		{
			setPosition(x, prevWindowPos.y)
			shouldShift = false
		}
		Gdx.graphics.requestRendering()
	}
	
	private fun adjustPosition(height: Int): Boolean
	{
		val focusedTextField = stage?.keyboardFocus as? GTextField ?: return false
		stage.stageToScreenCoordinates(windowScreenPos.set(x, y))
		focusedTextField.localToScreenCoordinates(textFieldScreenPos.set(0F, 0F))
		
		val screenHeight = Gdx.graphics.height - height
		if (textFieldScreenPos.y > screenHeight) // TextField is off-screen
		{
			val diff = textFieldScreenPos.y - screenHeight
			windowScreenPos.y -= diff
			stage.screenToStageCoordinates(windowScreenPos)
			setPosition(windowScreenPos.x, windowScreenPos.y)
			return true
		}
		return false
	}
	
	override fun hide(action: Action?)
	{
		stage?.unfocusAll()
		super.hide(action)
	}
}
