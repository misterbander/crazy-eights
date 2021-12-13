package misterbander.crazyeights.scene2d.dialogs

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField
import kotlin.coroutines.cancellation.CancellationException

@Suppress("BlockingMethodInNonBlockingContext")
class JoinRoomDialog(mainMenu: MainMenu) : PlayDialog(mainMenu, "Join Room")
{
	private val ipTextField = scene2d.gTextField(this@JoinRoomDialog, "", FORM_TEXT_FIELD_STYLE)
	private val joinButton = scene2d.textButton("Join", TEXT_BUTTON_STYLE) {
		usernameTextField.setTextFieldListener { textField, c -> isDisabled = textField.text.isBlank() }
		onChange {
			mainMenu.click.play()
			hide()
			mainMenu.messageDialog.show("Join Room", "Joining room...", "Cancel") {
				if (mainMenu.transition.isRunning)
					return@show
				info("JoinRoomDialog | INFO") { "Cancelling connection..." }
				if (mainMenu.clientListener != null)
				{
					game.network.client?.removeListener(mainMenu.clientListener!!)
					mainMenu.clientListener = null
				}
				joinRoomJob?.cancel()
				game.network.stop()
				show()
			}
			joinRoomJob = KtxAsync.launch {
				val ip = ipTextField.text
				val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else 11530
				try
				{
					val room = game.getScreen<Room>()
					room.clientListener = room.ClientListener()
					mainMenu.clientListener = mainMenu.ClientListener()
					val client = game.network.createAndConnectClient(ip, port)
					client.addListener(mainMenu.clientListener!!)
					client.addListener(room.clientListener)
					// Perform handshake by doing checking version and username availability
					info("Client | INFO") { "Perform handshake" }
					client.sendTCP(Handshake(data = arrayOf(game.user.name)))
				}
				catch (e: Exception)
				{
					if (e !is CancellationException && joinRoomJob?.isCancelled == false)
					{
						mainMenu.messageDialog.show("Error", e.toString(), "OK", this@JoinRoomDialog::show)
						game.network.stop()
					}
				}
			}
		}
	}
	
	private var joinRoomJob: Job? = null
	
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
			add(joinButton).prefWidth(224F)
			add(scene2d.textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { mainMenu.click.play(); hide() }
			})
		}
		addListener(UnfocusListener(this))
	}
	
	override fun show()
	{
		super.show()
		joinButton.isDisabled = usernameTextField.text.isBlank()
	}
}
