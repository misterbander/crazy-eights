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
					game.client?.removeListener(room.clientListener)
					game.network.stop()
					room.transition.start(targetScreen = game.getScreen<MainMenuScreen>())
				}
			}
		})
	}
}
