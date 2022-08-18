package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.COLOR_BUTTON_STYLE
import misterbander.crazyeights.DEFAULT_TCP_PORT
import misterbander.crazyeights.FORM_TEXT_FIELD_STYLE
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.MainMenuScreen
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.net.packets.Handshake
import misterbander.gframework.scene2d.GTextField
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField
import java.net.BindException

@Suppress("BlockingMethodInNonBlockingContext")
class CreateRoomDialog(private val mainMenu: MainMenuScreen) : RebuildableDialog(mainMenu, "Create Room")
{
	private val colorButton: ImageButton = scene2d.imageButton(COLOR_BUTTON_STYLE) {
		onChange { mainMenu.click.play(); colorPickerDialog.show() }
	}
	
	private val colorPickerDialog = ColorPickerDialog(mainMenu, colorButton)
	
	private var port = "11530"
	private var roomCode = (1..6).map { (('A'..'Z') + ('a'..'z') + ('0'..'9')).random() }.joinToString("")
	private var showAdvancedOptions = false
	private var createRoomJob: Job? = null
	
	override fun build()
	{
		val createButton = scene2d.textButton("Create") {
			isDisabled = game.user.name.isEmpty()
			onChange {
				mainMenu.click.play()
				hide()
				createRoom()
			}
		}
		contentTable.add(scene2d.table {
			defaults().left().space(16F)
			label("Username:", LABEL_SMALL_STYLE)
			gTextField(this@CreateRoomDialog, game.user.name, FORM_TEXT_FIELD_STYLE) {
				maxLength = 20
				onChange {
					game.user = game.user.copy(name = text)
					createButton.isDisabled = text.isEmpty()
				}
			}.cell(preferredWidth = 416F)
			actor(colorButton) { image.color.fromHsv(game.user.color.toHsv(FloatArray(3))[0], 0.8F, 0.8F) }
			row()
			if (showAdvancedOptions)
			{
				label("Server Port:", LABEL_SMALL_STYLE)
				gTextField(this@CreateRoomDialog, port, FORM_TEXT_FIELD_STYLE) {
					textFieldFilter = GTextField.GTextFieldFilter.DigitsOnlyFilter()
					onChange {  port = text }
				}.cell(preferredWidth = 416F)
				row()
			}
			label("Room Code:", LABEL_SMALL_STYLE)
			gTextField(this@CreateRoomDialog, roomCode, FORM_TEXT_FIELD_STYLE) {
				onChange {  roomCode = text }
			}.cell(preferredWidth = 416F)
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			actor(createButton).cell(preferredWidth = 248F)
			textButton(if (showAdvancedOptions) "Simple" else "Advanced") {
				onChange {
					mainMenu.click.play()
					showAdvancedOptions = !showAdvancedOptions
					rebuild()
				}
			}.cell(preferredWidth = 224F)
			textButton("Cancel") {
				onChange { mainMenu.click.play(); hide() }
			}
		})
		addListener(UnfocusListener(this))
	}
	
	private fun createRoom()
	{
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
			val port = if (port.isNotEmpty()) port.toInt() else DEFAULT_TCP_PORT
			try
			{
				game.network.createAndStartServer(roomCode, port)
				if (!isActive)
					throw CancellationException()
				val room = game.getScreen<RoomScreen>()
				room.clientListener = room.ClientListener()
				val client = game.network.createAndConnectClient("localhost", port)
				client.addListener(mainMenu.ClientListener())
				client.addListener(room.clientListener)
				// Perform handshake by doing checking version and username availability
				info("Client | INFO") { "Perform handshake" }
				client.sendTCP(Handshake(data = arrayOf(game.user.name, roomCode)))
			}
			catch (e: Exception)
			{
				if (e !is CancellationException && createRoomJob?.isCancelled == false)
				{
					if (e is BindException)
						mainMenu.messageDialog.show(
							"Error",
							"Port address $port is already in use.",
							"OK",
							this@CreateRoomDialog::show
						)
					else
						mainMenu.messageDialog.show("Error", e.toString(), "OK", this@CreateRoomDialog::show)
					game.network.stop()
				}
			}
		}
	}
	
	override fun hide()
	{
		super.hide()
		game.savePreferences()
	}
}
