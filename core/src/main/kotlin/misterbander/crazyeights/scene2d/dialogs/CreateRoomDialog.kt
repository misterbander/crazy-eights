package misterbander.crazyeights.scene2d.dialogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.UnfocusListener
import java.net.BindException

@Suppress("BlockingMethodInNonBlockingContext")
class CreateRoomDialog(mainMenu: MainMenu) : RoomSettingsDialog(mainMenu, "Create Room")
{
	private var createServerJob: Job? = null
	
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
						info("JoinRoomDialog | INFO") { "Cancelling creation..." }
						createServerJob?.cancel()
						game.network.stop()
						show()
					}
					createServerJob = KtxAsync.launch {
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							game.network.createAndStartServer(port)
							val client = game.network.createAndConnectClient("localhost", port)
							client.addListener(mainMenu.ClientListener())
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							client.sendTCP(Handshake(data = arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							if (e !is CancellationException && createServerJob?.isCancelled == false)
							{
								if (e is BindException)
									mainMenu.messageDialog.show("Error", "Port address $port is already in use.", "OK", this@CreateRoomDialog::show)
								else
									mainMenu.messageDialog.show("Error", e.toString(), "OK", this@CreateRoomDialog::show)
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
