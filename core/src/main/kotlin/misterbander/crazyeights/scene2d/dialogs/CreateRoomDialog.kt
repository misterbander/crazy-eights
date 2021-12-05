package misterbander.crazyeights.scene2d.dialogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.UnfocusListener
import java.net.BindException

@Suppress("BlockingMethodInNonBlockingContext")
class CreateRoomDialog(mainMenu: MainMenu) : RoomSettingsDialog(mainMenu, "Create Room")
{
	private var createRoomJob: Job? = null
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(scene2d.label("Username:", INFO_LABEL_STYLE_S))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(scene2d.label("Server Port:", INFO_LABEL_STYLE_S))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Create", TEXT_BUTTON_STYLE) {
				onChange {
					mainMenu.click.play()
					hide()
					mainMenu.messageDialog.show("Create Room", "Creating room...", "Cancel") {
						if (mainMenu.transition.isRunning)
							return@show
						info("JoinRoomDialog | INFO") { "Cancelling room creation..." }
						if (mainMenu.clientListener != null)
						{
							game.network.client?.removeListener(mainMenu.clientListener!!)
							mainMenu.clientListener = null
						}
						createRoomJob?.cancel()
						game.network.stop()
						show()
					}
					createRoomJob = KtxAsync.launch {
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							game.network.createAndStartServer(port)
							if (!isActive)
								throw CancellationException()
							val room = game.getScreen<Room>()
							room.clientListener = room.ClientListener()
							val client = game.network.createAndConnectClient("localhost", port)
							client.addListener(mainMenu.ClientListener())
							client.addListener(room.clientListener)
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							client.sendTCP(Handshake(data = arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							if (e !is CancellationException && createRoomJob?.isCancelled == false)
							{
								if (e is BindException)
									mainMenu.messageDialog.show("Error", "Port address $port is already in use.", "OK", this@CreateRoomDialog::show)
								else
									mainMenu.messageDialog.show("Error", e.toString(), "OK", this@CreateRoomDialog::show)
								game.network.stop()
							}
						}
					}
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { mainMenu.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
}
