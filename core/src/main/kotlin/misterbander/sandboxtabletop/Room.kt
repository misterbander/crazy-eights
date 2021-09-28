package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveActorAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.Timer
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.KtxInputListener
import ktx.actors.onChange
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.setScrollFocus
import ktx.actors.then
import ktx.async.interval
import ktx.graphics.use
import ktx.log.info
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.gTextField
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.textSize
import misterbander.gframework.util.toPixmap
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.net.cursorPositionPool
import misterbander.sandboxtabletop.net.objectMovedEventPool
import misterbander.sandboxtabletop.net.objectRotatedEventPool
import misterbander.sandboxtabletop.net.packets.FlipCardEvent
import misterbander.sandboxtabletop.net.packets.ObjectLockEvent
import misterbander.sandboxtabletop.net.packets.ObjectMovedEvent
import misterbander.sandboxtabletop.net.packets.ObjectRotatedEvent
import misterbander.sandboxtabletop.net.packets.ObjectUnlockEvent
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent
import misterbander.sandboxtabletop.scene2d.Card
import misterbander.sandboxtabletop.scene2d.Draggable
import misterbander.sandboxtabletop.scene2d.Gizmo
import misterbander.sandboxtabletop.scene2d.Lockable
import misterbander.sandboxtabletop.scene2d.Rotatable
import misterbander.sandboxtabletop.scene2d.SandboxTabletopCursor
import misterbander.sandboxtabletop.scene2d.SmoothMovable
import misterbander.sandboxtabletop.scene2d.Tabletop
import misterbander.sandboxtabletop.scene2d.dialogs.GameMenuDialog
import kotlin.math.min

class Room(game: SandboxTabletop) : SandboxTabletopScreen(game), Listener
{
	// Shaders
	val brightenShader = game.assetStorage[Shaders.brighten]
	private val vignetteShader = game.assetStorage[Shaders.vignette]
	
	// UI
	private val gameMenuDialog = GameMenuDialog(this)
	
	private val menuButton = scene2d.imageButton(MENU_BUTTON_STYLE) {
		onChange { click.play(); gameMenuDialog.show() }
	}
	private val chatTextField = scene2d.gTextField(null, "", CHAT_TEXT_FIELD_STYLE) {
		messageText = if (Gdx.app.type == Application.ApplicationType.Android) "Tap here to chat..." else "Press T to chat..."
		maxLength = 256
		setFocusTraversal(false)
		onKey { character ->
			if ((character == '\r' || character == '\n') && text.isNotEmpty())
			{
				game.client?.sendTCP(Chat(game.user, "<${game.user.username}> $text", false))
				text = ""
				uiStage.keyboardFocus = null
			}
		}
		onKeyboardFocus { focused ->
			chatPopup.isVisible = !focused
			chatHistoryScrollPane.isVisible = focused
			Gdx.input.setOnscreenKeyboardVisible(focused)
		}
	}
	private val chatHistory = scene2d.verticalGroup {
		grow()
		onTouchDown { Gdx.input.setOnscreenKeyboardVisible(false) }
	}
	private val chatHistoryScrollPane = scene2d.scrollPane(SCROLL_PANE_STYLE) {
		actor = chatHistory
		isVisible = false
	}
	private val chatPopup = scene2d.verticalGroup { columnAlign(Align.left) }
	val gizmo1 = Gizmo(game.shapeDrawer, Color.GREEN) // TODO ###### remove debug
	val gizmo2 = Gizmo(game.shapeDrawer, Color.CYAN)
	
	// Tabletop states
	val tabletop = Tabletop(this)
	
//	val hand: Hand = Hand(this)
//	private val handRegion: TextureRegion = game.skin.getRegion("hand")
	
	// Networking
	private var cursorPosition = CursorPosition()
	val objectMovedEvents = IntMap<ObjectMovedEvent>()
	val objectRotatedEvents = IntMap<ObjectRotatedEvent>()
	private var syncServerTask: Timer.Task? = null
	private var tickDelay = 1/40F
	var selfDisconnect = false
	
	init
	{
		uiStage += object : Actor()
		{
			init
			{
				addListener(object : KtxInputListener()
				{
					override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean
					{
						uiStage.keyboardFocus = null
						Gdx.input.setOnscreenKeyboardVisible(false)
						return false
					}
				})
			}
			
			override fun hit(x: Float, y: Float, touchable: Boolean): Actor = this
		}
		uiStage += scene2d.table {
			setFillParent(true)
			top()
			actor(menuButton).cell(pad = 16F).inCell.top()
			table {
				defaults().growX()
				actor(chatTextField)
				row()
				stack {
					container(chatPopup).top().left()
					actor(chatHistoryScrollPane)
				}.inCell.left()
			}.cell(pad = 16F, expandX = true, fillX = true, maxHeight = 312F)
		}
		uiStage.scrollFocus = chatHistoryScrollPane
		uiStage.addListener(object : KtxInputListener()
		{
			override fun keyDown(event: InputEvent, keycode: Int): Boolean
			{
				if (event.keyCode == Input.Keys.T && !chatTextField.hasKeyboardFocus())
				{
					Gdx.app.postRunnable { uiStage.keyboardFocus = chatTextField }
					return true
				}
				else if (event.keyCode == Input.Keys.ESCAPE)
				{
					uiStage.keyboardFocus = null
					return true
				}
				return false
			}
		})
		stage += tabletop.cards
		stage += tabletop.cursors
		stage += gizmo1
		stage += gizmo2
		
//		stage.addActor(new Debug(viewport, game.getShapeDrawer()));
	}
	
	override fun show()
	{
		super.show()
		
		if (Gdx.app.type == Application.ApplicationType.Desktop)
		{
			val cursorBorder: TextureRegion = Scene2DSkin.defaultSkin["cursorborder"]
			val cursorBorderPixmap = cursorBorder.toPixmap()
			val cursorBase: TextureRegion = Scene2DSkin.defaultSkin["cursorbase"]
			val cursorBasePixmap = cursorBase.toPixmap()
			for (i in 0 until cursorBasePixmap.width)
			{
				for (j in 0 until cursorBasePixmap.height)
				{
					val color = Color(cursorBasePixmap.getPixel(i, j))
					cursorBasePixmap.setColor(color.mul(game.user.color))
					cursorBasePixmap.drawPixel(i, j)
				}
			}
			cursorBorderPixmap.drawPixmap(cursorBasePixmap, 0, 0)
			Gdx.graphics.setCursor(Gdx.graphics.newCursor(cursorBorderPixmap, 3, 0))
			cursorBorderPixmap.dispose()
			cursorBasePixmap.dispose()
		}
		
		cursorPosition.username = game.user.username
		syncServerTask = interval(tickDelay, tickDelay) {
			val (inputX, inputY) = stage.screenToStageCoordinates(tempVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
			if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
			{
				cursorPosition.x = inputX
				cursorPosition.y = inputY
				tabletop.myCursor?.setTargetPosition(cursorPosition.x, cursorPosition.y)
				game.client?.sendTCP(cursorPosition)
			}
			objectMovedEvents.forEach { game.client?.sendTCP(it.value); objectMovedEventPool.free(it.value) }
			objectRotatedEvents.forEach { game.client?.sendTCP(it.value); objectRotatedEventPool.free(it.value) }
			objectMovedEvents.clear()
			objectRotatedEvents.clear()
		}
	}
	
	override fun clearScreen()
	{
		game.batch.use {
			it.shader = vignetteShader
			game.shapeDrawer.setColor(BACKGROUND_COLOR)
			game.shapeDrawer.filledRectangle(0F, 0F, viewport.worldWidth, viewport.worldHeight)
			it.shader = null
			it.color = Color.WHITE
//			it.draw(handRegion, 0F, 0F, viewport.worldWidth, 96F) TODO hand region
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun resize(width: Int, height: Int)
	{
		super.resize(width, height)
		chatPopup.children.forEach {
			val chatLabelContainer = it as Container<Label>
			val label = chatLabelContainer.actor!!
			chatLabelContainer.width(label.style.font.chatTextWidth(label.text.toString()))
			chatLabelContainer.invalidateHierarchy()
		}
		vignetteShader.bind()
		vignetteShader.setUniformf("u_resolution", width.toFloat(), height.toFloat())
	}
	
	/**
	 * Appends a chat message to the chat history, and adds a chat label that disappears after 5 seconds.
	 * @param message the message
	 * @param color   color of the chat message
	 */
	fun chat(message: String, color: Color? = null)
	{
		val chatLabel = scene2d.label(message, CHAT_LABEL_STYLE) {
			wrap = true
			if (color != null)
				this.color = color.cpy()
		}
		chatPopup += scene2d.container(chatLabel) {
			width(chatLabel.style.font.chatTextWidth(message))
			val alphaAction = AlphaAction().apply { alpha = 0F; duration = 1F }
			val removeActorAction = RemoveActorAction().apply { target = this@container } // Action to remove label after fade out
			this += DelayAction(10F) then alphaAction then removeActorAction
		}
		
		if (chatPopup.children.size == 7) // Maximum 6 children
		{
			val firstChatPopup: Actor = chatPopup.removeActorAt(0, false)
			firstChatPopup.clear()
		}
		
		// Add to history
		val chatHistoryLabel = scene2d.label(message, INFO_LABEL_STYLE) {
			wrap = true
			if (color != null)
				this.color = color.cpy()
		}
		chatHistory.pad(4F, 16F, 4F, 16F).space(8F)
		chatHistory += chatHistoryLabel
		chatHistoryScrollPane.layout()
		chatHistoryScrollPane.scrollPercentY = 100F
	}
	
	private fun BitmapFont.chatTextWidth(message: String): Float =
		min(textSize(message).x + 32, uiViewport.worldWidth - menuButton.width - 64)
	
	override fun disconnected(connection: Connection)
	{
		val mainMenu = game.getScreen<MainMenu>()
		if (!selfDisconnect)
			mainMenu.messageDialog.show("Disconnected", "Server closed.", "OK")
		transition.start(targetScreen = mainMenu)
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		when (`object`)
		{
			is UserJoinEvent -> Gdx.app.postRunnable {
				val user = `object`.user
				if (user != game.user)
					tabletop += user
				chat("${user.username} joined the game", Color.YELLOW)
			}
			is UserLeaveEvent -> Gdx.app.postRunnable {
				val user = `object`.user
				tabletop -= user
				chat("${user.username} left the game", Color.YELLOW)
			}
			is Chat -> Gdx.app.postRunnable {
				chat(`object`.message, if (`object`.isSystemMessage) Color.YELLOW else null)
				info("Client | CHAT") { `object`.message }
			}
			is CursorPosition ->
			{
				val cursor: SandboxTabletopCursor? = tabletop.userCursorMap[`object`.username]
				cursor?.setTargetPosition(`object`.x, `object`.y)
				cursorPositionPool.free(`object`)
			}
			is ObjectLockEvent -> Gdx.app.postRunnable { // User attempts to lock an object
				val (id, lockerUsername) = `object`
				val toLock = tabletop.idGObjectMap[id]!!
				val lockable = toLock.getModule<Lockable>()
				if (lockable != null && !lockable.isLocked)
				{
					toLock.toFront()
					toLock.setScrollFocus()
					lockable.lockHolder = tabletop.users[lockerUsername]
				}
			}
			is ObjectUnlockEvent -> Gdx.app.postRunnable {
				val toUnlock = tabletop.idGObjectMap[`object`.id]
				toUnlock.getModule<Lockable>()?.lockHolder = null
				toUnlock.getModule<Draggable>()?.justDragged = false
				toUnlock.getModule<Rotatable>()?.justRotated = false
			}
			is ObjectMovedEvent ->
			{
				val (id, x, y) = `object`
				tabletop.idGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { setTargetPosition(x, y) }
				objectMovedEventPool.free(`object`)
			}
			is ObjectRotatedEvent ->
			{
				val (id, rotation) = `object`
				tabletop.idGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { rotationInterpolator.target = rotation }
				objectRotatedEventPool.free(`object`)
			}
			is FlipCardEvent -> Gdx.app.postRunnable {
				val card = tabletop.idGObjectMap[`object`.id] as Card
				card.isFaceUp = !card.isFaceUp
			}
		}
	}
	
	override fun hide()
	{
		chatPopup.clearChildren()
		chatHistory.clearChildren()
		
		tabletop.reset()
		
		cursorPosition.reset()
		objectMovedEvents.clear()
		objectRotatedEvents.clear()
		syncServerTask?.cancel()
		syncServerTask = null
		selfDisconnect = false
		
		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
		game.stopNetwork()
	}
}
