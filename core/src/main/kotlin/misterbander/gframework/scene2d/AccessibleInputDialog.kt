package misterbander.gframework.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import ktx.actors.onKeyboardFocus
import ktx.math.vec2
import misterbander.gframework.GScreen

/**
 * Makes [Dialog]s that contain text fields accessible on mobile such that while editing them, the window gets shifted
 * upwards, so it doesn't get covered by the on-screen keyboard
 *
 * For this to work, a layout size listener should be attached in Android that calls `GFramework::notifySizeChange()`,
 * and the window must be added to the [GScreen]'s accessible window list.
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
	
	fun addFocusListener(mbTextField: MBTextField)
	{
		mbTextField.onKeyboardFocus { focused -> if (focused) adjustPosition(keyboardHeight) }
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
		val focusedTextField: MBTextField = stage?.keyboardFocus as? MBTextField ?: return false
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
}
