package misterbander.crazyeights.scene2d.dialogs

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
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
import misterbander.gframework.scene2d.GTextWidget
import misterbander.gframework.scene2d.UnfocusListener
import misterbander.gframework.scene2d.gTextField
import java.net.ConnectException
import kotlin.coroutines.cancellation.CancellationException

class JoinRoomDialog(private val mainMenu: MainMenuScreen) : RebuildableDialog(mainMenu, "Join Room")
{
	private val colorButton: ImageButton = scene2d.imageButton(COLOR_BUTTON_STYLE) {
		onChange { mainMenu.click.play(); colorPickerDialog.show() }
	}
	
	private val colorPickerDialog = ColorPickerDialog(mainMenu, colorButton)
	
	private var serverAddress = ""
	private var port = "11530"
	private var roomCode = ""
	private var showAdvancedOptions = false
	private var joinRoomJob: Job? = null
	
	override fun build()
	{
		val joinButton = scene2d.textButton("Join") {
			isDisabled = game.user.name.isEmpty()
			onChange {
				mainMenu.click.play()
				hide()
				joinRoom()
			}
		}
		contentTable.add(scene2d.table {
			defaults().left().space(16F)
			label("Username:", LABEL_SMALL_STYLE)
			gTextField(this@JoinRoomDialog, game.user.name, FORM_TEXT_FIELD_STYLE) {
				maxLength = 20
				onChange {
					game.user = game.user.copy(name = text)
					joinButton.isDisabled = text.isEmpty()
				}
			}.cell(preferredWidth = 416F)
			actor(colorButton) { image.color.fromHsv(game.user.color.toHsv(FloatArray(3))[0], 0.8F, 0.8F) }
			row()
			if (showAdvancedOptions)
			{
				label("Server Address:", LABEL_SMALL_STYLE)
				gTextField(this@JoinRoomDialog, serverAddress, FORM_TEXT_FIELD_STYLE) {
					onChange {  serverAddress = text }
				}.cell(preferredWidth = 416F)
				row()
				label("Server Port:", LABEL_SMALL_STYLE)
				gTextField(this@JoinRoomDialog, port, FORM_TEXT_FIELD_STYLE) {
					filter = GTextWidget.GTextWidgetFilter.DigitsOnlyFilter()
					onChange {  port = text }
				}.cell(preferredWidth = 416F)
				row()
			}
			label("Room Code:", LABEL_SMALL_STYLE)
			gTextField(this@JoinRoomDialog, roomCode, FORM_TEXT_FIELD_STYLE) {
				onChange {  roomCode = text }
			}.cell(preferredWidth = 416F)
			textButton("How to Join?") {
				onChange {
					mainMenu.click.play()
					Gdx.net.openURI("https://github.com/misterbander/crazy-eights/wiki/Multiplayer-Guide")
				}
			}
		})
		buttonTable.add(scene2d.table {
			defaults().space(16F)
			actor(joinButton).cell(preferredWidth = 248F)
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
	
	private fun joinRoom()
	{
		mainMenu.messageDialog.show("Join Room", "Joining room...", "Cancel") {
			if (mainMenu.transition.isRunning)
				return@show
			info("JoinRoomDialog | INFO") { "Cancelling connection..." }
			if (mainMenu.listener != null)
				game.net.client?.removeListener(mainMenu)
			joinRoomJob?.cancel()
			game.net.stop()
			show()
		}
		joinRoomJob = KtxAsync.launch {
			val port = if (port.isNotEmpty()) port.toInt() else DEFAULT_TCP_PORT
			try
			{
				val room = game.getScreen<RoomScreen>()
				val client = if (showAdvancedOptions)
					game.net.createAndConnectClient(serverAddress, port, maxRetries = 5)
				else
				{
					info("Client | INFO") { "Finding server with room code $roomCode" }
					game.net.createAndConnectClientByRoomCode(roomCode)
				}
				if (!isActive)
					throw CancellationException()
				client.addListener(mainMenu)
				client.addListener(room)
				// Perform handshake by doing checking version and username availability
				info("Client | INFO") { "Perform handshake" }
				client.sendTCP(Handshake(data = arrayOf(game.user.name, roomCode)))
			}
			catch (e: Exception)
			{
				if (e !is CancellationException && joinRoomJob?.isCancelled == false)
				{
					if (e is ConnectException && e.message?.startsWith("Couldn't find server with room code:") == true)
						mainMenu.messageDialog.show(
							"Error",
							"Couldn't find room with room code: $roomCode\n" +
									"Either the room code is incorrect, the server is not discoverable in your " +
									"network, or the server failed to respond in time.",
							"OK",
							this@JoinRoomDialog::show
						)
					else
						mainMenu.messageDialog.show("Error", e.toString(), "OK", this@JoinRoomDialog::show)
					game.net.stop()
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
