package misterbander.sandboxtabletop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.alpha
import ktx.actors.onChange
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.newSingleThreadAsyncContext
import ktx.collections.plusAssign
import ktx.graphics.use
import ktx.log.info
import ktx.scene2d.*
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.Handshake
import misterbander.sandboxtabletop.net.packets.HandshakeReject
import misterbander.sandboxtabletop.net.packets.RoomState
import misterbander.sandboxtabletop.scene2d.CreateRoomDialog
import misterbander.sandboxtabletop.scene2d.InfoDialog
import misterbander.sandboxtabletop.scene2d.JoinRoomDialog
import misterbander.sandboxtabletop.scene2d.MessageDialog

class MenuScreen(game: SandboxTabletop) : SandboxTabletopScreen(game), Listener
{
	private val createRoomDialog = CreateRoomDialog(this)
	private val joinRoomDialog = JoinRoomDialog(this)
	val messageDialog = MessageDialog(this)
	val infoDialog = InfoDialog(this)
	
	private val mainTable: Table by lazy {
		scene2d.table {
			defaults().prefWidth(224F).space(16F)
			textButton("Play", TEXT_BUTTON_STYLE) {
				onChange { click.play(); showTable(playTable) }
			}
			row()
			textButton("Quit", TEXT_BUTTON_STYLE) {
				onChange { click.play(); Gdx.app.exit() }
			}
		}
	}
	private val playTable: Table by lazy {
		scene2d.table {
			defaults().prefWidth(224F).space(16F)
			textButton("Create Room", TEXT_BUTTON_STYLE) {
				onChange { click.play(); createRoomDialog.show() }
			}
			row()
			textButton("Join Room", TEXT_BUTTON_STYLE) {
				onChange { click.play(); joinRoomDialog.show() }
			}
			row()
			textButton("Cancel", TEXT_BUTTON_STYLE) {
				onChange { click.play(); showTable(mainTable) }
			}
		}
	}
	private var activeTable: Table
	
	val asyncContext = newSingleThreadAsyncContext("MenuScreen-AsyncExecutor-Thread")
	
	init
	{
		val logo: Texture = game.assetManager["textures/logo.png"]
		logo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
		uiStage += scene2d.table {
			setFillParent(true)
			image(logo).cell(pad = 16F).inCell.top()
			row()
			stack {
				actor(mainTable)
				actor(playTable)
			}.cell(expand = true)
		}
		activeTable = mainTable
		playTable.alpha = 0F
		playTable.isVisible = false
		
		keyboardHeightObservers += createRoomDialog
		keyboardHeightObservers += joinRoomDialog
	}
	
	private fun showTable(table: Table)
	{
		uiStage.addCaptureListener(ignoreTouchDown)
		activeTable += Actions.alpha(0F) then Actions.run {
			activeTable.isVisible = false
			table.isVisible = true
			table += Actions.fadeIn(ANIMATION_DURATION, Interpolation.exp5Out) then Actions.run {
				uiStage.removeCaptureListener(ignoreTouchDown)
				activeTable = table
			}
		}
	}
	
	override fun clearScreen()
	{
		Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1F)
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
		game.batch.use(uiCamera) {
			val shapeDrawer = game.shapeDrawer
			shapeDrawer.update()
			shapeDrawer.setColor(ACCENT_COLOR)
			shapeDrawer.filledRectangle(
				80F, 96F, viewport.worldWidth - 96, viewport.worldHeight - 96,
				4*MathUtils.degRad
			)
		}
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		infoDialog.hide()
		when (`object`)
		{
			is Handshake -> // Handshake is successful
			{
				info("Client | INFO") { "Handshake successful. Joining as ${game.user.username} (UUID: ${game.user.uuid})..." }
				connection.sendTCP(game.user)
			}
			is HandshakeReject ->
			{
				info("Client | INFO") { "Handshake failed, reason: ${`object`.reason}" }
				Network.stop()
				infoDialog.hide()
				messageDialog.show("Error", `object`.reason, "OK", joinRoomDialog::show)
			}
			is RoomState ->
			{
				val roomScreen = game.getScreen<RoomScreen>()
				roomScreen.state = `object`
				Network.client!!.removeListener(this)
				Network.client!!.addListener(roomScreen)
				transition.start(targetScreen = roomScreen)
			}
		}
	}
}
