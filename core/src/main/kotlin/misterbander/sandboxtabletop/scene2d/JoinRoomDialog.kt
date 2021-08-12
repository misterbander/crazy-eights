package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.esotericsoftware.kryonet.Client
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktx.actors.isShown
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.gframework.scene2d.MBTextField
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.sandboxtabletop.FORM_TEXT_FIELD_STYLE
import misterbander.sandboxtabletop.INFO_LABEL_STYLE
import misterbander.sandboxtabletop.MenuScreen
import misterbander.sandboxtabletop.TEXT_BUTTON_STYLE
import misterbander.sandboxtabletop.VERSION_STRING
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.Handshake
import kotlin.coroutines.cancellation.CancellationException

@Suppress("BlockingMethodInNonBlockingContext")
class JoinRoomDialog(screen: MenuScreen) : RoomDialog(screen, "Join Room")
{
	private val ipTextField = MBTextField(this@JoinRoomDialog, "", game.skin, FORM_TEXT_FIELD_STYLE)
	private var joinServerJob: Job? = null
	
	init
	{
		contentTable.apply {
			defaults().left().space(16F)
			add(Label("Username:", game.skin, INFO_LABEL_STYLE))
			add(usernameTextField).prefWidth(288F)
			add(colorButton)
			row()
			add(Label("Server IP Address:", game.skin, INFO_LABEL_STYLE))
			add(ipTextField).prefWidth(288F)
			row()
			add(Label("Server Port:", game.skin, INFO_LABEL_STYLE))
			add(portTextField).prefWidth(288F)
		}
		buttonTable.apply {
			add(scene2d.textButton("Join", TEXT_BUTTON_STYLE, game.skin) {
				onChange {
					screen.click.play()
					hide()
					screen.messageDialog.show("Join Room", "Joining room...", "Cancel") {
						joinServerJob?.cancel()
						joinServerJob = null
						Network.stop()
						show()
					}
					joinServerJob = KtxAsync.launch {
						val ip = ipTextField.text
						val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
						try
						{
							Network.stopJob?.await()
							Network.client = Client()
							withContext(screen.asyncContext) {
								Network.client!!.apply {
									addListener(screen)
									start()
									connect(ip, port)
								}
							}
							// Perform handshake by doing checking version and username availability
							info("Client | INFO") { "Perform handshake" }
							Network.client!!.sendTCP(Handshake(VERSION_STRING, arrayOf(game.user.username)))
						}
						catch (e: Exception)
						{
							Network.stop()
							screen.infoDialog.hide()
							if (e !is CancellationException && !isShown())
								screen.messageDialog.show("Error", e.toString(), "OK", this@JoinRoomDialog::show)
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
