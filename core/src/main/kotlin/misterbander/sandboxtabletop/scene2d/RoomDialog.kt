package misterbander.sandboxtabletop.scene2d

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.gframework.scene2d.MBTextField
import misterbander.gframework.scene2d.mbTextField
import misterbander.sandboxtabletop.COLOR_BUTTON_STYLE
import misterbander.sandboxtabletop.FORM_TEXT_FIELD_STYLE
import misterbander.sandboxtabletop.MenuScreen

abstract class RoomDialog(screen: MenuScreen, title: String) : SandboxTabletopDialog(screen, title)
{
	val usernameTextField by lazy {
		scene2d.mbTextField(this, "", FORM_TEXT_FIELD_STYLE) { maxLength = 20 }
	}
	val colorButton = scene2d.imageButton(COLOR_BUTTON_STYLE) {
		onChange { screen.click.play(); colorPickerDialog.show() }
	}
	val portTextField by lazy {
		scene2d.mbTextField(this, "", FORM_TEXT_FIELD_STYLE) {
			textFieldFilter = MBTextField.MBTextFieldFilter.DigitsOnlyFilter()
		}
	}
	
	private val colorPickerDialog by lazy { ColorPickerDialog(screen, this) }
	
	override fun show()
	{
		super.show()
		usernameTextField.text = game.user.username
		colorButton.image.color.fromHsv(game.userColor.toHsv(FloatArray(3))[0], 0.8F, 0.8F)
	}
	
	override fun hide()
	{
		super.hide()
		game.user = game.user.copy(username = usernameTextField.text)
		game.savePreferences()
	}
}
