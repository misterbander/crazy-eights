package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE

class GameMenuDialog(room: Room) : CrazyEightsDialog(room, "Game Menu")
{
	init
	{
		contentTable.pad(0F)
		buttonTable.apply {
			defaults().center().space(16F)
			padTop(16F)
			add(scene2d.textButton("Continue", TEXT_BUTTON_STYLE) { onChange { room.click.play(); hide() } })
			row()
			add(scene2d.textButton("Quit", TEXT_BUTTON_STYLE) {
				onChange {
					room.click.play()
					hide()
					room.selfDisconnect = true
					game.client?.removeListener(room.clientListener)
					game.network.stop()
					room.transition.start(targetScreen = game.getScreen<MainMenu>())
				}
			})
		}
	}
}