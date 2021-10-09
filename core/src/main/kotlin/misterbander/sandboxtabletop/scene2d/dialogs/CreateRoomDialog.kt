package misterbander.sandboxtabletop.scene2d.dialogs

import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.sandboxtabletop.INFO_LABEL_STYLE_S
import misterbander.sandboxtabletop.MainMenu
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.Handshake
import java.net.BindException

@Suppress("BlockingMethodInNonBlockingContext")
class CreateRoomDialog(mainMenu: MainMenu) : RoomSettingsDialog(mainMenu, "Create Room")
{
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
					mainMenu.infoDialog.show("Create Room", "Creating room...")
					KtxAsync.launch {
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							info("Network | INFO") { "Starting server on port $port..." }
							game.stopNetworkJob?.await()
							val server: Server
							val client: Client
							withContext(mainMenu.asyncContext) {
								server = Server()
								server.start()
								server.bind(port)
								client = Client()
								client.addListener(mainMenu.ClientListener())
								client.start()
								client.connect("localhost", port)
								game.network = Network(server, client)
							}
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							client.sendTCP(Handshake(data = arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							mainMenu.infoDialog.hide()
							if (e is BindException)
								mainMenu.messageDialog.show("Error", "Port address $port is already in use.", "OK", this@CreateRoomDialog::show)
							else
								mainMenu.messageDialog.show("Error", e.toString(), "OK", this@CreateRoomDialog::show)
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
