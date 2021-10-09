package misterbander.sandboxtabletop.scene2d.dialogs

import com.esotericsoftware.kryonet.Client
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField
import misterbander.sandboxtabletop.FORM_TEXT_FIELD_STYLE
import misterbander.sandboxtabletop.INFO_LABEL_STYLE_S
import misterbander.sandboxtabletop.MainMenu
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.Handshake
import kotlin.coroutines.cancellation.CancellationException

@Suppress("BlockingMethodInNonBlockingContext")
class JoinRoomDialog(mainMenu: MainMenu) : RoomSettingsDialog(mainMenu, "Join Room")
{
	private val ipTextField = scene2d.gTextField(this@JoinRoomDialog, "", FORM_TEXT_FIELD_STYLE)
	private var joinServerJob: Job? = null
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(scene2d.label("Username:", INFO_LABEL_STYLE_S))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(scene2d.label("Server IP Address:", INFO_LABEL_STYLE_S))
			add(ipTextField).prefWidth(288F)
			row()
			add(scene2d.label("Server Port:", INFO_LABEL_STYLE_S))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Join", TEXT_BUTTON_STYLE) {
				onChange {
					mainMenu.click.play()
					hide()
					mainMenu.messageDialog.show("Join Room", "Joining room...", "Cancel") {
						info("JoinRoomDialog | INFO") { "Cancelling connection..." }
						joinServerJob?.cancel()
						joinServerJob = null
						game.stopNetwork()
						show()
					}
					joinServerJob = KtxAsync.launch {
						val ip = ipTextField.text
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							game.stopNetworkJob?.await()
							val client = Client()
							game.network = Network(null, client)
							withContext(mainMenu.asyncContext) {
								client.addListener(mainMenu.ClientListener())
								client.start()
								client.connect(ip, port)
							}
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							client.sendTCP(Handshake(data = arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							game.stopNetwork()
							mainMenu.infoDialog.hide()
							if (e !is CancellationException && !isShown())
								mainMenu.messageDialog.show("Error", e.toString(), "OK", this@JoinRoomDialog::show)
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
