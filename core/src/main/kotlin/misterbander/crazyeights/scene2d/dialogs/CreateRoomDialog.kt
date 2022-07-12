package misterbander.crazyeights.scene2d.dialogs

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.DEFAULT_TCP_PORT
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.MainMenu
import misterbander.crazyeights.Room
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.UnfocusListener
import java.net.BindException

@Suppress("BlockingMethodInNonBlockingContext")
class CreateRoomDialog(mainMenu: MainMenu, isAdvanced: Boolean) : PlayDialog(mainMenu, "Create Room")
{
	private val createButton = scene2d.textButton("Create") {
		usernameTextField.setTextFieldListener { textField, _ -> isDisabled = textField.text.isBlank() }
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
				val port = if (portTextField.text.isNotEmpty()) portTextField.text.toInt() else DEFAULT_TCP_PORT
				try
				{
					game.network.createAndStartServer(roomCodeTextField.text, port)
					if (!isActive)
						throw CancellationException()
					val room = game.getScreen<Room>()
					room.clientListener = room.ClientListener()
					val client = game.network.createAndConnectClient("localhost", port)
					client.addListener(mainMenu.ClientListener())
					client.addListener(room.clientListener)
					// Perform handshake by doing checking version and username availability
					info("Client | INFO") { "Perform handshake" }
					client.sendTCP(Handshake(data = arrayOf(game.user.name, roomCodeTextField.text)))
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
	}
	private var createRoomJob: Job? = null
	
	init
	{
		contentTable.add(scene2d.table {
			defaults().left().space(16F)
			label("Username:", LABEL_SMALL_STYLE)
			actor(usernameTextField).cell(preferredWidth = 416F)
			actor(colorButton)
			row()
			if (isAdvanced)
			{
				label("Server Port:", LABEL_SMALL_STYLE)
				actor(portTextField).cell(preferredWidth = 416F)
				row()
			}
			label("Room Code:", LABEL_SMALL_STYLE)
			actor(roomCodeTextField).cell(preferredWidth = 416F)
			val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
			roomCodeTextField.text = (1..6).map { chars.random() }.joinToString("")
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			actor(createButton).cell(preferredWidth = 248F)
			textButton(if (isAdvanced) "Simple" else "Advanced") {
				onChange {
					mainMenu.click.play()
					hide()
					if (isAdvanced)
						mainMenu.createRoomDialog.show()
					else
						mainMenu.advancedCreateRoomDialog.show()
				}
			}.cell(preferredWidth = 224F)
			textButton("Cancel") {
				onChange { mainMenu.click.play(); hide() }
			}
		})
		addListener(UnfocusListener(this))
	}
	
	override fun show()
	{
		super.show()
		createButton.isDisabled = usernameTextField.text.isBlank()
	}
}
