package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.actors.txt
import ktx.scene2d.*
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.model.User
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent

class UserDialog(room: Room) : CrazyEightsDialog(room, "User Info")
{
	var user = User("")
		set(value)
		{
			field = value
			usernameLabel.txt = value.username
			usernameLabel.color = value.color
		}
	private val usernameLabel = scene2d.label("", INFO_LABEL_STYLE_S)
	private val swapSeatsButton = scene2d.textButton("Swap Seats", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			game.client?.sendTCP(SwapSeatsEvent(game.user.username, user.username))
			hide()
		}
	}
	private val removeButton = scene2d.textButton("Remove", TEXT_BUTTON_STYLE) {
		onChange {
			room.click.play()
			game.client?.sendTCP(AiRemoveEvent(user.username))
			hide()
		}
	}
	private val horizontalGroup = scene2d.horizontalGroup {
		space(16F)
		actor(swapSeatsButton)
		actor(removeButton)
		textButton("Cancel", TEXT_BUTTON_STYLE) {
			onChange { room.click.play(); hide() }
		}
	}
	
	init
	{
		contentTable.add(usernameLabel)
		buttonTable.apply {
			add(horizontalGroup)
		}
	}
	
	fun show(user: User)
	{
		this.user = user
		if (user.isAi)
			horizontalGroup.addActorAfter(swapSeatsButton, removeButton)
		else
			horizontalGroup.removeActor(removeButton)
		show()
	}
}
