package misterbander.sandboxtabletop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import ktx.actors.alpha
import ktx.actors.onChange
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.plusAssign
import ktx.graphics.use
import ktx.scene2d.*
import misterbander.sandboxtabletop.scene2d.JoinRoomDialog

class MenuScreen(game: SandboxTabletop) : SandboxTabletopScreen(game)
{
//	var client: SandboxTabletopClient? = null
	
	private val joinRoomDialog = JoinRoomDialog(this)
//	val connectingDialog = MessageDialog(this, "")
	
	private val mainTable: Table by lazy {
		scene2d.table {
			defaults().prefWidth(224F).space(16F)
			textButton("Play", "textbuttonstyle", game.skin) {
				onChange { click.play(); showTable(playTable) }
			}
			row()
			textButton("Quit", "textbuttonstyle", game.skin) {
				onChange { click.play(); Gdx.app.exit() }
			}
		}
	}
	private val playTable: Table by lazy {
		scene2d.table {
			defaults().prefWidth(224F).space(16F)
			textButton("Create Room", "textbuttonstyle", game.skin) {
				onChange { click.play(); }
			}
			row()
			textButton("Join Room", "textbuttonstyle", game.skin) {
				onChange { click.play(); joinRoomDialog.show() }
			}
			row()
			textButton("Cancel", "textbuttonstyle", game.skin) {
				onChange { click.play(); showTable(mainTable) }
			}
		}
	}
	private var activeTable: Table
	private val ignoreTouchDown: InputListener = object : InputListener()
	{
		override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
		{
			event.cancel()
			return false
		}
	}
	
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
		
		keyboardHeightObservers += joinRoomDialog
	}
	
	private fun showTable(table: Table)
	{
		uiStage.addCaptureListener(ignoreTouchDown)
		activeTable += Actions.fadeOut(ANIMATION_DURATION, Interpolation.exp5In) then Actions.run {
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
		game.batch.use {
			game.shapeDrawer.setColor(ACCENT_COLOR)
			game.shapeDrawer.filledRectangle(
				80F, 96F, viewport.worldWidth - 96, viewport.worldHeight - 96,
				4 * MathUtils.degRad
			)
		}
	}
	
//	fun connectionOpened(connection: Connection)
//	{
//		Gdx.app.log("SandboxTabletopClient | INFO", "Connected to " + connection.remoteAddress)
//		assert(client != null)
//		val user = User(connectWindow.usernameTextField.getText(), game.uuid)
//		val roomScreen = RoomScreen(game, client, user)
//		client.send(user)
//		client.setConnectionEventListener(roomScreen)
//		game.setScreen<KtxScreen>(roomScreen)
//		Gdx.app.log("SandboxTabletopClient | INFO", "Joining game as $user")
//	}
//
//	fun connectionClosed(connection: Connection, e: Exception?)
//	{
//		Gdx.app.log("SandboxTabletopClient | INFO", "Disconnected from " + connection.remoteAddress)
//		client = null
//	}
//
//	fun connectionFailed(e: IOException)
//	{
//		// Exception occurred because cannot connect to remote server
//		connectingDialog.show(
//			"Connection Failed", "Failed to connect to server.\n$e"
//				+ if (e.cause != null) """
//
// 	${e.cause}
// 	""".trimIndent() else "", "OK", connectWindow::show
//		)
//		e.printStackTrace()
//	}
}
