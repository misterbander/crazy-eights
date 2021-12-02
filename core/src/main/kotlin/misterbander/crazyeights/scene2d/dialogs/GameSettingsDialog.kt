package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.AiAddEvent

class GameSettingsDialog(room: Room) : CrazyEightsDialog(room, "Game Settings")
{
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(scene2d.textButton("Add AI", TEXT_BUTTON_STYLE) {
				onChange {
					room.click.play()
					hide()
					game.client?.sendTCP(AiAddEvent)
				}
			})
		}
		buttonTable.apply {
			add(scene2d.textButton("OK", TEXT_BUTTON_STYLE) {
				onChange { room.click.play(); hide() }
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { room.click.play(); hide() }
			})
		}
	}
}
