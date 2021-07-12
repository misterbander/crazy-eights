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
	private val ipTextField = MBTextField(this@JoinRoomDialog, "", game.skin, "formtextfieldstyle")
	private val portTextField = scene2d.mbTextField(this@JoinRoomDialog, "", "formtextfieldstyle", game.skin) {
		textFieldFilter = MBTextField.MBTextFieldFilter.DigitsOnlyFilter()
	}
	
	init
	{
		contentTable.apply {
			pad(16F)
			defaults().left().space(16F)
			add(Label("Username:", game.skin, "infolabelstyle"))
			add(usernameTextField).prefWidth(288F)
			row()
			add(Label("Server IP Address:", game.skin, "infolabelstyle"))
			add(ipTextField).prefWidth(288F)
			row()
			add(Label("Server Port:", game.skin, "infolabelstyle"))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			pad(0F, 16F, 16F, 16F)
			defaults().space(16F)
			add(scene2d.textButton("Join", "textbuttonstyle", game.skin) {
				onChange { screen.click.play(); }
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", "textbuttonstyle", game.skin) {
				onChange { screen.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
}
