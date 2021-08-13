package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveActorAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Timer
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.onChange
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.async.interval
import ktx.collections.minusAssign
import ktx.collections.plusAssign
import ktx.graphics.use
import ktx.log.info
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.mbTextField
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.textSize
import misterbander.gframework.util.toPixmap
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.CursorPosition
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.cursorPositionPool
import misterbander.sandboxtabletop.net.packets.RoomState
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import misterbander.sandboxtabletop.net.packets.UserLeaveEvent
import misterbander.sandboxtabletop.scene2d.GameMenuDialog
import misterbander.sandboxtabletop.scene2d.SandboxTabletopCursor
import misterbander.sandboxtabletop.scene2d.Tabletop
import kotlin.math.min

class RoomScreen(game: SandboxTabletop) : SandboxTabletopScreen(game), Listener
{
	// UI
	private val gameMenuDialog = GameMenuDialog(this)
	
	private val menuButton = scene2d.imageButton(MENU_BUTTON_STYLE) {
		onChange { click.play(); gameMenuDialog.show() }
	}
	private val chatTextField = scene2d.mbTextField(null, "", CHAT_TEXT_FIELD_STYLE) {
		messageText = if (Gdx.app.type == Application.ApplicationType.Android) "Tap here to chat..." else "Press T to chat..."
		maxLength = 256
		setFocusTraversal(false)
		onKey { character ->
			if ((character == '\r' || character == '\n') && text.isNotEmpty())
			{
				Network.client?.sendTCP(Chat(game.user, "<${game.user.username}> $text", false))
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
	private val shader = ShaderProgram(
		Gdx.files.internal("shaders/passthrough.vsh").readString(),
		Gdx.files.internal("shaders/vignette.fsh").readString()
	).apply {
		if (!isCompiled)
			ktx.log.error("RoomScreen | ERROR") { log }
	}
	var selfDisconnect = false
	
	// Room and client states
	private val tabletop = Tabletop()
	var state: RoomState = RoomState()
	private val cursorPosition = CursorPosition()
	private var updateCursorTask: Timer.Task? = null
	
//	@Null
//	var latestServerObjectPosition: ServerObjectPosition? = null
//	val uuidActorMap: ObjectMap<UUID, Actor> = ObjectMap<UUID, Actor>()
//	val hand: Hand = Hand(this)
//	private val handRegion: TextureRegion = game.skin.getRegion("hand")
	
	private val tps = 1/40F
	
	init
	{
		uiStage += object : Actor()
		{
			init
			{
				onTouchDown {
					uiStage.keyboardFocus = null
					Gdx.input.setOnscreenKeyboardVisible(false)
				}
			}
			
			override fun hit(x: Float, y: Float, touchable: Boolean): Actor
			{
				return this
			}
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
		uiStage.addListener(object : InputListener()
		{
			override fun keyDown(event: InputEvent, keycode: Int): Boolean
			{
				if (event.keyCode == Input.Keys.T && !chatTextField.hasKeyboardFocus())
				{
					Gdx.app.postRunnable { uiStage.keyboardFocus = chatTextField }
					return true
				} else if (event.keyCode == Input.Keys.ESCAPE)
				{
					uiStage.keyboardFocus = null
					return true
				}
				return false
			}
		})
		stage += tabletop.cursors
		
//		stage.addActor(new Debug(viewport, game.getShapeDrawer()));
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
		shader.bind()
		shader.setUniformf("u_resolution", width.toFloat(), height.toFloat())
	}
	
	override fun show()
	{
		super.show()
		val user = game.user
		
		// Set up cursors
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
					cursorBasePixmap.setColor(color.mul(user.color))
					cursorBasePixmap.drawPixel(i, j)
				}
			}
			cursorBorderPixmap.drawPixmap(cursorBasePixmap, 0, 0)
			Gdx.graphics.setCursor(Gdx.graphics.newCursor(cursorBorderPixmap, 3, 0))
			cursorBorderPixmap.dispose()
			cursorBasePixmap.dispose()
		}
		state.users.forEach { // Add cursor for other users
			if (it != user)
				tabletop.addCursor(it.username, SandboxTabletopCursor(it))
		}
		if (Gdx.app.type != Application.ApplicationType.Desktop) // Add own cursor only if not on desktop
		{
			tabletop.myCursor = SandboxTabletopCursor(user)
			tabletop.addCursor(user.username, tabletop.myCursor!!)
		}
		cursorPosition.username = user.username
		updateCursorTask = interval(tps, tps) {
			val (inputX, inputY) = stage.screenToStageCoordinates(tempVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
			if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
			{
				cursorPosition.x = inputX
				cursorPosition.y = inputY
				tabletop.myCursor?.setTargetPosition(cursorPosition.x, cursorPosition.y)
				Network.client?.sendTCP(cursorPosition)
//				if (latestServerObjectPosition != null)
//				{
//					client.send(latestServerObjectPosition)
//					latestServerObjectPosition = null
//				}
			}
		}
	}
	
	/**
	 * Appends a chat message to the chat history, and adds a chat label that disappears after 5 seconds.
	 * @param message the message
	 * @param color   color of the chat message
	 */
	private fun chat(message: String, color: Color? = null)
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
	
	private fun BitmapFont.chatTextWidth(message: String): Float
	{
		return min(textSize(message).x + 32, uiViewport.worldWidth - menuButton.width - 64)
	}
	
	override fun disconnected(connection: Connection)
	{
		val menuScreen = game.getScreen<MenuScreen>()
		if (!selfDisconnect)
			menuScreen.messageDialog.show("Disconnected", "Server closed.", "OK")
		transition.start(targetScreen = menuScreen)
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		when (`object`)
		{
			is UserJoinEvent ->
			{
				val user = `object`.user
				if (user != game.user)
				{
					state.users += user
					tabletop.addCursor(user.username, SandboxTabletopCursor(user))
				}
				chat("${user.username} joined the game", Color.YELLOW)
			}
			is UserLeaveEvent ->
			{
				val user = `object`.user
				state.users -= user
				tabletop.removeCursor(user.username)
				chat("${user.username} left the game", Color.YELLOW)
			}
			is Chat ->
			{
				chat(`object`.message, if (`object`.isSystemMessage) Color.YELLOW else null)
				info("Client | CHAT") { `object`.message }
			}
			is CursorPosition ->
			{
				val cursor: SandboxTabletopCursor? = tabletop.userCursorMap[`object`.username]
				cursor?.setTargetPosition(`object`.x, `object`.y)
				cursorPositionPool.free(`object`)
			}
		}
	}
	
//	fun objectReceived(connection: Connection?, `object`: Serializable)
//	{
//		if (`object` is ServerObjectList)
//		{
//			val objectList: Array<ServerObject> = (`object` as ServerObjectList).objects
//			for (i in objectList.indices)
//			{
//				val serverObject: ServerObject = objectList[i]
//				if (serverObject is ServerCard)
//				{
//					val serverCard: ServerCard = serverObject as ServerCard
//					val card = Card(
//						this,
//						serverCard.getUUID(),
//						serverCard.rank,
//						serverCard.suit,
//						serverCard.lockHolder,
//						serverCard.owner,
//						serverCard.getX(),
//						serverCard.getY(),
//						serverCard.getRotation(),
//						serverCard.isFaceUp
//					)
//					uuidActorMap.put(serverCard.getUUID(), card)
//					stage.addActor(card)
//					card.setZIndex(i)
//					if (card.owner != null)
//					{
//						if (user.equals(card.owner)) hand.addCard(card) else card.setVisible(false)
//					}
//				}
//			}
//			println("Arranging cards")
//			hand.arrangeCards(false)
//		}
//		else if (`object` is LockEvent)
//		{
//			val event: LockEvent = `object` as LockEvent
//			val actor: Actor = uuidActorMap.get<UUID>(event.lockedUuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				card.setZIndex(uuidActorMap.size)
//				card.lockHolder = event.lockHolder
//			}
//		}
//		else if (`object` is OwnerEvent)
//		{
//			val event: OwnerEvent = `object` as OwnerEvent
//			val actor: Actor = uuidActorMap.get<UUID>(event.ownedUuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				card.owner = event.owner
//				card.setVisible(card.owner == null || card.owner.equals(user))
//			}
//		}
//		else if (`object` is ServerObjectPosition)
//		{
//			val serverObjectPosition: ServerObjectPosition = `object` as ServerObjectPosition
//			val actor: Actor = uuidActorMap.get<UUID>(serverObjectPosition.uuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				if (card.owner == null) card.setTargetPosition(serverObjectPosition.x, serverObjectPosition.y)
//			}
//		}
//		else if (`object` is FlipCardEvent)
//		{
//			val flipCardEvent: FlipCardEvent = `object` as FlipCardEvent
//			val actor: Actor = uuidActorMap.get<UUID>(flipCardEvent.uuid)
//			if (actor is Card)
//			{
//				val card: Card = actor as Card
//				card.setFaceUp(flipCardEvent.isFaceUp)
//				if (card.owner == null) card.setZIndex(uuidActorMap.size)
//			}
//		}
//	}
	
	override fun render(delta: Float)
	{
		transitionCamera.update()
		transition.update(delta)
		clearScreen()
		game.batch.use {
			it.shader = shader
			game.shapeDrawer.setColor(BACKGROUND_COLOR)
			game.shapeDrawer.filledRectangle(0F, 0F, viewport.worldWidth, viewport.worldHeight)
			it.shader = null
			it.color = Color.WHITE
//			it.draw(handRegion, 0F, 0F, viewport.worldWidth, 96F) TODO hand region
		}
		renderStage(camera, stage, delta)
		renderStage(uiCamera, uiStage, delta)
		updateWorld()
		transition.render()
	}
	
	override fun hide()
	{
		chatPopup.clearChildren()
		chatHistory.clearChildren()
		selfDisconnect = false
		tabletop.reset()
		updateCursorTask?.cancel()
		updateCursorTask = null
		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
		Network.stop()
	}
	
	override fun dispose()
	{
		super.dispose()
		shader.dispose()
	}
}
