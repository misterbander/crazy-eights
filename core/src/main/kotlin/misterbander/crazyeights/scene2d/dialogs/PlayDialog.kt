package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.COLOR_BUTTON_STYLE
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.MainMenu
import misterbander.gframework.scene2d.GTextField
import misterbander.gframework.scene2d.gTextField

abstract class PlayDialog(mainMenu: MainMenu, title: String) : CrazyEightsDialog(mainMenu, title)
{
	val usernameTextField by lazy {
		scene2d.gTextField(this, "", FORM_TEXT_FIELD_STYLE) { maxLength = 20 }
	}
	val colorButton = scene2d.imageButton(COLOR_BUTTON_STYLE) {
		onChange { mainMenu.click.play(); colorPickerDialog.show() }
	}
	val portTextField by lazy {
		scene2d.gTextField(this, "11530", FORM_TEXT_FIELD_STYLE) {
			textFieldFilter = GTextField.GTextFieldFilter.DigitsOnlyFilter()
		}
	}
	val roomCodeTextField by lazy {
		scene2d.gTextField(this, "", FORM_TEXT_FIELD_STYLE)
	}
	
	private val colorPickerDialog by lazy { ColorPickerDialog(mainMenu, this) }
	
	override fun show()
	{
		super.show()
		usernameTextField.text = game.user.name
		colorButton.image.color.fromHsv(game.user.color.toHsv(FloatArray(3))[0], 0.8F, 0.8F)
	}
	
	override fun hide()
	{
		super.hide()
		game.user = game.user.copy(name = usernameTextField.text)
		game.savePreferences()
	}
}
