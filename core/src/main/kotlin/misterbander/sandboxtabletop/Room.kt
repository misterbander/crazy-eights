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
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.OrderedSet
import com.badlogic.gdx.utils.Queue
import com.badlogic.gdx.utils.Timer
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.KtxInputListener
import ktx.actors.onChange
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.interval
import ktx.collections.minusAssign
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
import misterbander.sandboxtabletop.scene2d.Gizmo
import misterbander.sandboxtabletop.scene2d.SandboxTabletopCursor
import misterbander.sandboxtabletop.scene2d.Tabletop
import misterbander.sandboxtabletop.scene2d.dialogs.GameMenuDialog
import misterbander.sandboxtabletop.scene2d.modules.Lockable
import misterbander.sandboxtabletop.scene2d.modules.SmoothMovable
import kotlin.math.min

class Room(game: SandboxTabletop) : SandboxTabletopScreen(game)
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
				uiStage.scrollFocus = null
			}
		}
		onKeyboardFocus { focused ->
			chatPopup.isVisible = !focused
			chatHistoryScrollPane.isVisible = focused
			uiStage.scrollFocus = if (focused) chatHistoryScrollPane else null
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
	var clientListener = ClientListener()
	
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
						uiStage.scrollFocus = null
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
		uiStage.addListener(object : KtxInputListener()
		{
			override fun keyDown(event: InputEvent, keycode: Int): Boolean
			{
				if (event.keyCode == Input.Keys.T && !chatTextField.hasKeyboardFocus())
				{
					Gdx.app.postRunnable {
						uiStage.keyboardFocus = chatTextField
						uiStage.scrollFocus = chatHistoryScrollPane
					}
					return true
				}
				else if (event.keyCode == Input.Keys.ESCAPE)
				{
					uiStage.keyboardFocus = null
					uiStage.scrollFocus = null
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
		
		clientListener.startSyncServerTask()
	}
	
	override fun render(delta: Float)
	{
		super.render(delta)
		while (clientListener.incomingPacketBuffer.notEmpty())
			clientListener.processPacket(clientListener.incomingPacketBuffer.removeFirst())
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
			this += delay(10F) then alpha(0F, 1F) then Actions.removeActor(this)
		}
		
		if (chatPopup.children.size == 7) // Maximum 6 children
		{
			val firstChatPopup: Actor = chatPopup.removeActorAt(0, false)
			firstChatPopup.clear()
		}
		
		// Add to history
		val chatHistoryLabel = scene2d.label(message, INFO_LABEL_STYLE_S) {
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
	
	override fun hide()
	{
		chatPopup.clearChildren()
		chatHistory.clearChildren()
		tabletop.reset()
		
		clientListener.stopSyncServerTask()
		
		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
		game.stopNetwork()
	}
	
	inner class ClientListener : Listener
	{
		private var cursorPosition = CursorPosition()
		val outgoingPacketBuffer = OrderedSet<Any>()
		val incomingPacketBuffer = Queue<Any>()
		
		private var syncServerTask: Timer.Task? = null
		private var tickDelay = 1/40F
		var selfDisconnect = false
		
		fun startSyncServerTask()
		{
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
				
				outgoingPacketBuffer.forEach { event: Any ->
					game.client?.sendTCP(event)
					if (event is ObjectMovedEvent)
						objectMovedEventPool.free(event)
					else if (event is ObjectRotatedEvent)
						objectRotatedEventPool.free(event)
				}
				outgoingPacketBuffer.clear()
			}
		}
		
		fun stopSyncServerTask()
		{
			syncServerTask?.cancel()
			syncServerTask = null
		}
		
		inline fun <reified T> removeFromOutgoingPacketBuffer(crossinline predicate: (T) -> Boolean): T?
		{
			val event = outgoingPacketBuffer.find { it is T && predicate(it) }
			if (event != null)
				outgoingPacketBuffer -= event
			return event as T?
		}
		
		override fun disconnected(connection: Connection)
		{
			val mainMenu = game.getScreen<MainMenu>()
			if (!selfDisconnect)
				mainMenu.messageDialog.show("Disconnected", "Server closed.", "OK")
			transition.start(targetScreen = mainMenu)
		}
		
		override fun received(connection: Connection, `object`: Any) = incomingPacketBuffer.addLast(`object`)
		
		fun processPacket(packet: Any)
		{
			val idToGObjectMap = tabletop.idToGObjectMap
			when (packet)
			{
				is UserJoinEvent ->
				{
					val user = packet.user
					if (user != game.user)
						tabletop += user
					chat("${user.username} joined the game", Color.YELLOW)
				}
				is UserLeaveEvent ->
				{
					val user = packet.user
					tabletop -= user
					chat("${user.username} left the game", Color.YELLOW)
				}
				is Chat ->
				{
					chat(packet.message, if (packet.isSystemMessage) Color.YELLOW else null)
					info("Client | CHAT") { packet.message }
				}
				is CursorPosition ->
				{
					val cursor: SandboxTabletopCursor? = tabletop.userToCursorMap[packet.username]
					if (cursor != tabletop.myCursor)
						cursor?.setTargetPosition(packet.x, packet.y)
					cursorPositionPool.free(packet)
				}
				is ObjectLockEvent -> // User attempts to lock an object
				{
					val (id, lockerUsername) = packet
					idToGObjectMap[id]!!.getModule<Lockable>()?.lock(tabletop.users[lockerUsername])
				}
				is ObjectUnlockEvent -> idToGObjectMap[packet.id].getModule<Lockable>()?.unlock()
				is ObjectMovedEvent ->
				{
					val (id, x, y, moverUsername) = packet
					if (game.user.username != moverUsername)
						idToGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { setTargetPosition(x, y) }
					objectMovedEventPool.free(packet)
				}
				is ObjectRotatedEvent ->
				{
					val (id, rotation, rotatorUsername) = packet
					if (game.user.username != rotatorUsername)
						idToGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { rotationInterpolator.target = rotation }
					objectRotatedEventPool.free(packet)
				}
				is FlipCardEvent ->
				{
					val card = idToGObjectMap[packet.id] as Card
					card.isFaceUp = !card.isFaceUp
				}
			}
		}
	}
}
