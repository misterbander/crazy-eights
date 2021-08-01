package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.gframework.scene2d.MBTextField
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.mbTextField
import misterbander.sandboxtabletop.MenuScreen

class JoinRoomDialog(screen: MenuScreen) : SandboxTabletopDialog(screen, "Join Room")
{
	val usernameTextField = scene2d.mbTextField(this, "", "formtextfieldstyle", game.skin) {
		maxLength = 20
	}
	val colorButton = scene2d.imageButton("colorbuttonstyle", game.skin) {
		image.color.fromHsv(game.userColor.toHsv(FloatArray(3))[0], 0.8F, 0.8F)
		onChange { screen.click.play(); colorPickerDialog.show() }
	}
	private val ipTextField = MBTextField(this@JoinRoomDialog, "", game.skin, "formtextfieldstyle")
	private val portTextField = scene2d.mbTextField(this@JoinRoomDialog, "", "formtextfieldstyle", game.skin) {
		textFieldFilter = MBTextField.MBTextFieldFilter.DigitsOnlyFilter()
	}
	
	private val colorPickerDialog = ColorPickerDialog(screen, this)
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(Label("Username:", game.skin, "infolabelstyle"))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(Label("Server IP Address:", game.skin, "infolabelstyle"))
			add(ipTextField).prefWidth(288F)
			row()
			add(Label("Server Port:", game.skin, "infolabelstyle"))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Join", "textbuttonstyle", game.skin) {
				onChange {
					screen.click.play()
					hide()
					screen.messageDialog.show("Join Room", "Joining room...", "Cancel") {
						screen.joinRoomDialog.show()
					}
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", "textbuttonstyle", game.skin) {
				onChange { screen.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
}
