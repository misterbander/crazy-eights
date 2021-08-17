package misterbander.sandboxtabletop.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.RoomScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE
import misterbander.sandboxtabletop.net.Network

class GameMenuDialog(screen: RoomScreen) : SandboxTabletopDialog(screen, "Game Menu")
{
	init
	{
		contentTable.pad(0F)
		buttonTable.apply {
			defaults().center().space(16F)
			padTop(16F)
			add(scene2d.textButton("Continue", TEXT_BUTTON_STYLE) { onChange { screen.click.play(); hide() } })
			row()
			add(scene2d.textButton("Quit", TEXT_BUTTON_STYLE) {
				onChange {
					screen.click.play()
					hide()
					screen.selfDisconnect = true
					Network.client!!.removeListener(screen.clientListener)
					Network.stop()
					screen.transition.start(targetScreen = screen.game.getScreen<MenuScreen>())
				}
			})
		}
	}
}
