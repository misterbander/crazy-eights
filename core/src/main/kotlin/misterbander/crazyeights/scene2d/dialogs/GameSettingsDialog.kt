package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.scene2d.*
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.AiAddEvent
import misterbander.crazyeights.net.packets.NewGameEvent

class GameSettingsDialog(private val room: Room) : CrazyEightsDialog(room, "Game Settings")
{
	private val newGameButton = scene2d.textButton("New Game", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			hide()
			game.client?.sendTCP(NewGameEvent())
		}
	}
	private val addAiButton = scene2d.textButton("Add AI", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			hide()
			game.client?.sendTCP(AiAddEvent)
		}
	}
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(newGameButton).center()
			row()
			add(addAiButton).center()
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
	
	override fun show()
	{
		super.show()
		newGameButton.isDisabled = room.isGameStarted
		addAiButton.isDisabled = room.isGameStarted
	}
}
