package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.MainMenuScreen
import misterbander.crazyeights.RoomScreen

class GameMenuDialog(room: RoomScreen) : CrazyEightsDialog(room, "Game Menu")
{
	init
	{
		contentTable.pad(0F)
		buttonTable.add(scene2d.table {
			defaults().center().space(16F)
			padTop(16F)
			textButton("Continue") { onChange { room.click.play(); hide() } }
			row()
			textButton("Quit") {
				onChange {
					room.click.play()
					hide()
					room.selfDisconnect = true
					game.client?.removeListener(room)
					game.net.stop()
					val mainMenu = game.getScreen<MainMenuScreen>()
					room.transition.start(targetScreen = mainMenu, targetScreenTransition = mainMenu.transition)
				}
			}
		})
	}
}
