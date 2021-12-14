package misterbander.crazyeights

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ScreenUtils
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.alpha
import ktx.actors.onChange
import ktx.actors.onKeyDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.*
import ktx.graphics.use
import ktx.log.info
import ktx.scene2d.*
import misterbander.crazyeights.model.ServerTabletop
import misterbander.crazyeights.net.packets.Handshake
import misterbander.crazyeights.net.packets.HandshakeReject
import misterbander.crazyeights.scene2d.dialogs.CreateRoomDialog
import misterbander.crazyeights.scene2d.dialogs.JoinRoomDialog
import misterbander.crazyeights.scene2d.dialogs.MessageDialog

class MainMenu(game: CrazyEights) : CrazyEightsScreen(game), Listener
{
	private val logo = game.assetStorage[Textures.title].apply {
		setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
	}
	
	val createRoomDialog = CreateRoomDialog(this, false)
	val advancedCreateRoomDialog = CreateRoomDialog(this, true)
	val joinRoomDialog = JoinRoomDialog(this, false)
	val advancedJoinRoomDialog = JoinRoomDialog(this, true)
	val messageDialog = MessageDialog(this)
	
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
	
	var clientListener: ClientListener? = null
	
	init
	{
		activeTable = mainTable
		val fallbackActor = Actor().apply {
			onKeyDown { keyCode ->
				if (keyCode != Input.Keys.BACK)
					return@onKeyDown
				if (activeTable != mainTable)
					showTable(mainTable)
				else
					Gdx.app.exit()
			}
		}
		uiStage += fallbackActor
		uiStage.keyboardFocus = fallbackActor
		uiStage += scene2d.table {
			setFillParent(true)
			image(logo).cell(pad = 16F).inCell.top()
			row()
			stack {
				actor(mainTable)
				actor(playTable)
				container(label(VERSION_STRING, INFO_LABEL_STYLE_S)) {
					bottom().right().padBottom(8F).padRight(8F)
				}
			}.cell(grow = true)
		}
		playTable.alpha = 0F
		playTable.isVisible = false
		
		keyboardHeightObservers += createRoomDialog
		keyboardHeightObservers += advancedCreateRoomDialog
		keyboardHeightObservers += joinRoomDialog
		keyboardHeightObservers += advancedJoinRoomDialog
	}
	
	private fun showTable(table: Table)
	{
		uiStage.addCaptureListener(ignoreTouchDown)
		activeTable += alpha(0F) then Actions.run {
			activeTable.isVisible = false
			table.isVisible = true
			table += fadeIn(ANIMATION_DURATION, Interpolation.exp5Out) then Actions.run {
				uiStage.removeCaptureListener(ignoreTouchDown)
				activeTable = table
			}
		}
	}
	
	override fun clearScreen()
	{
		ScreenUtils.clear(BACKGROUND_COLOR, true)
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
	
	inner class ClientListener : Listener
	{
		override fun received(connection: Connection, `object`: Any)
		{
			when (`object`)
			{
				is Handshake -> Gdx.app.postRunnable {
					// Handshake is successful
					info("Client | INFO") { "Handshake successful. Joining as ${game.user.name}..." }
					connection.sendTCP(game.user)
				}
				is HandshakeReject -> Gdx.app.postRunnable {
					info("Client | INFO") { "Handshake failed, reason: ${`object`.reason}" }
					game.network.stop()
					messageDialog.show("Error", `object`.reason, "OK", joinRoomDialog::show)
				}
				is ServerTabletop -> Gdx.app.postRunnable {
					val room = game.getScreen<Room>()
					room.tabletop.setState(`object`)
					messageDialog.actionlessHide()
					game.client!!.removeListener(this)
					transition.start(targetScreen = room)
				}
			}
		}
	}
}
