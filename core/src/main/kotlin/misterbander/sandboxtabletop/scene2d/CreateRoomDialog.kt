package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Server
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.sandboxtabletop.INFO_LABEL_STYLE
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE
import misterbander.sandboxtabletop.VERSION_STRING
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.Handshake
import java.net.BindException

class CreateRoomDialog(screen: MenuScreen) : RoomDialog(screen, "Create Room")
{
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(Label("Username:", game.skin, INFO_LABEL_STYLE))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(Label("Server Port:", game.skin, INFO_LABEL_STYLE))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Create", TEXT_BUTTON_STYLE, game.skin) {
				onChange {
					screen.click.play()
					hide()
					screen.infoDialog.show("Create Room", "Creating room...")
					KtxAsync.launch {
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							info("Network | INFO") { "Starting server on port $port..." }
							@Suppress("BlockingMethodInNonBlockingContext")
							withContext(game.asyncContext) {
								Network.server = Server().apply { start(); bind(port) }
								Network.client = Client().apply {
									addListener(screen)
									start()
									connect("localhost", port)
								}
							}
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							Network.client!!.sendTCP(Handshake(VERSION_STRING, arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							screen.infoDialog.hide()
							if (e is BindException)
								screen.messageDialog.show("Error", "Port address $port is already in use", "OK", this@CreateRoomDialog::show)
							else
								screen.messageDialog.show("Error", e.toString(), "OK", this@CreateRoomDialog::show)
						}
					}
				}
			}).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE, game.skin) {
				onChange { screen.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
}
