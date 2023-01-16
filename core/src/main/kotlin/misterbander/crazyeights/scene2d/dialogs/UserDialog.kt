package misterbander.crazyeights.scene2d.dialogs

import ktx.actors.onChange
import ktx.actors.txt
import ktx.scene2d.*
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.net.packets.AiRemoveEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.server.User
import misterbander.crazyeights.scene2d.Tabletop

class UserDialog(private val room: RoomScreen) : RebuildableDialog(room, "User Info")
{
	private val tabletop: Tabletop
		get() = room.tabletop
	var user = User("")
		private set
	private val swapSeatsButton = scene2d.textButton("Swap Seats") {
		onChange {
			room.click.play()
			game.client?.sendTCP(SwapSeatsEvent(game.user.name, user.name))
			hide()
		}
	}
	private val removeButton = scene2d.textButton("Remove") {
		onChange {
			room.click.play()
			game.client?.sendTCP(AiRemoveEvent(user.name))
			hide()
		}
	}
	
	override fun build()
	{
		contentTable.add(scene2d.label(user.name, LABEL_SMALL_STYLE) {
			txt = user.name
			color = user.color
		})
		buttonTable.add(scene2d.horizontalGroup {
			space(16F)
			actor(swapSeatsButton)
			if (user.isAi)
				actor(removeButton)
			textButton("Cancel") {
				onChange { room.click.play(); hide() }
			}
		})
	}
	
	fun show(user: User)
	{
		this.user = user
		show()
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		swapSeatsButton.isDisabled = tabletop.isGameStarted
		removeButton.isDisabled = tabletop.isGameStarted
	}
}
