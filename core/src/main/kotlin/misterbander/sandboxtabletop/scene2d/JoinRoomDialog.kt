package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.gframework.scene2d.MBTextField
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.sandboxtabletop.FORM_TEXT_FIELD_STYLE
import misterbander.sandboxtabletop.INFO_LABEL_STYLE
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE

class JoinRoomDialog(screen: MenuScreen) : RoomDialog(screen, "Join Room")
{
	private val ipTextField = MBTextField(this@JoinRoomDialog, "", game.skin, FORM_TEXT_FIELD_STYLE)
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(Label("Username:", game.skin, INFO_LABEL_STYLE))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(Label("Server IP Address:", game.skin, INFO_LABEL_STYLE))
			add(ipTextField).prefWidth(288F)
			row()
			add(Label("Server Port:", game.skin, INFO_LABEL_STYLE))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Join", TEXT_BUTTON_STYLE, game.skin) {
				onChange {
					screen.click.play()
					hide()
					screen.messageDialog.show("Join Room", "Joining room...", "Cancel") {
						screen.joinRoomDialog.show()
					}
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE, game.skin) {
				onChange { screen.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
}
