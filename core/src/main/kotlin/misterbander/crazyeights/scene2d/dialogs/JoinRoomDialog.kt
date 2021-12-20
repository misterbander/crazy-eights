package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.DEFAULT_TCP_PORT
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.INFO_LABEL_STYLE_S
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.Room
import misterbander.crazyeights.TEXT_BUTTON_STYLE
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField
import java.net.ConnectException
import kotlin.coroutines.cancellation.CancellationException

@Suppress("BlockingMethodInNonBlockingContext")
class JoinRoomDialog(mainMenu: MainMenu, isAdvanced: Boolean) : PlayDialog(mainMenu, "Join Room")
{
	private val ipTextField = scene2d.gTextField(this@JoinRoomDialog, "", FORM_TEXT_FIELD_STYLE)
	private val joinButton = scene2d.textButton("Join", TEXT_BUTTON_STYLE) {
		usernameTextField.setTextFieldListener { textField, _ -> isDisabled = textField.text.isBlank() }
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
				val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else DEFAULT_TCP_PORT
				try
				{
					val room = game.getScreen<Room>()
					room.clientListener = room.ClientListener()
					mainMenu.clientListener = mainMenu.ClientListener()
					val client = if (isAdvanced)
						game.network.createAndConnectClient(ip, port)
					else
					{
						info("Client | INFO") { "Finding server with room code ${roomCodeTextField.text}" }
						game.network.createAndConnectClientByRoomCode(roomCodeTextField.text)
					}
					if (!isActive)
						throw CancellationException()
					client.addListener(mainMenu.clientListener!!)
					client.addListener(room.clientListener)
					// Perform handshake by doing checking version and username availability
					info("Client | INFO") { "Perform handshake" }
					client.sendTCP(Handshake(data = arrayOf(game.user.name, roomCodeTextField.text)))
				}
				catch (e: Exception)
				{
					if (e !is CancellationException && joinRoomJob?.isCancelled == false)
					{
						if (e is ConnectException && e.message?.startsWith("Couldn't find server with room code:") == true)
							mainMenu.messageDialog.show(
								"Error",
								"Couldn't find room with room code: ${roomCodeTextField.text}\n" +
									"Either the room code is incorrect, the server is not discoverable in your " +
									"network, or the server failed to respond in time.",
								"OK",
								this@JoinRoomDialog::show
							)
						else
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
		contentTable.add(scene2d.table {
			defaults().left().space(16F)
			label("Username:", INFO_LABEL_STYLE_S)
			actor(usernameTextField).cell(preferredWidth = 416F)
			actor(colorButton)
			row()
			if (isAdvanced)
			{
				label("Server IP Address:", INFO_LABEL_STYLE_S)
				actor(ipTextField).cell(preferredWidth = 416F)
				row()
				label("Server Port:", INFO_LABEL_STYLE_S)
				actor(portTextField).cell(preferredWidth = 416F)
				row()
			}
			label("Room Code:", INFO_LABEL_STYLE_S)
			actor(roomCodeTextField).cell(preferredWidth = 416F)
			textButton("How to Join?", TEXT_BUTTON_STYLE) {
				onChange {
					mainMenu.click.play()
					Gdx.net.openURI("https://github.com/misterbander/crazy-eights/wiki/Multiplayer-Guide")
				}
			}
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			actor(joinButton).cell(preferredWidth = 248F)
			textButton(if (isAdvanced) "Simple" else "Advanced", TEXT_BUTTON_STYLE) {
				onChange {
					mainMenu.click.play()
					hide()
					if (isAdvanced)
						mainMenu.joinRoomDialog.show()
					else
						mainMenu.advancedJoinRoomDialog.show()
				}
			}.cell(preferredWidth = 224F)
			textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { mainMenu.click.play(); hide() }
			}
		})
		addListener(UnfocusListener(this))
	}
	
	override fun show()
	{
		super.show()
		joinButton.isDisabled = usernameTextField.text.isBlank()
	}
}
