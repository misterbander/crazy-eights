package misterbander.sandboxtabletop.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.gframework.scene2d.GTextField
import misterbander.gframework.scene2d.gTextField
import misterbander.sandboxtabletop.COLOR_BUTTON_STYLE
import misterbander.sandboxtabletop.FORM_TEXT_FIELD_STYLE
import misterbander.sandboxtabletop.MenuScreen

abstract class RoomDialog(screen: MenuScreen, title: String) : SandboxTabletopDialog(screen, title)
{
	val usernameTextField by lazy {
		scene2d.gTextField(this, "", FORM_TEXT_FIELD_STYLE) { maxLength = 20 }
	}
	val colorButton = scene2d.imageButton(COLOR_BUTTON_STYLE) {
		onChange { screen.click.play(); colorPickerDialog.show() }
	}
	val portTextField by lazy {
		scene2d.gTextField(this, "", FORM_TEXT_FIELD_STYLE) {
			textFieldFilter = GTextField.GTextFieldFilter.DigitsOnlyFilter()
		}
	}
	
	private val colorPickerDialog by lazy { ColorPickerDialog(screen, this) }
	
	override fun show()
	{
		super.show()
		usernameTextField.text = game.user.username
		colorButton.image.color.fromHsv(game.user.color.toHsv(FloatArray(3))[0], 0.8F, 0.8F)
	}
	
	override fun hide()
	{
		super.hide()
		game.user = game.user.copy(username = usernameTextField.text)
		game.savePreferences()
	}
}
