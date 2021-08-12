package misterbander.sandboxtabletop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.actions.RemoveActorAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.plusAssign
import ktx.graphics.use
import ktx.log.info
import ktx.scene2d.*
import ktx.style.*
import misterbander.gframework.scene2d.mbTextField
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.textSize
import misterbander.gframework.util.toPixmap
import misterbander.sandboxtabletop.model.Chat
import misterbander.sandboxtabletop.model.User
import misterbander.sandboxtabletop.net.Network
import misterbander.sandboxtabletop.net.packets.RoomState
import misterbander.sandboxtabletop.net.packets.UserJoinEvent
import kotlin.math.min

class RoomScreen(game: SandboxTabletop) : SandboxTabletopScreen(game), Listener
{
	private val menuButton = scene2d.imageButton(MENU_BUTTON_STYLE)
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
	)
	
	var state: RoomState = RoomState()
	
//	@Null
//	private var myCursor: Cursor? = null
//	private val cursorPosition: CursorPosition
//
//	@Null
//	var latestServerObjectPosition: ServerObjectPosition? = null
//	val uuidActorMap: ObjectMap<UUID, Actor> = ObjectMap<UUID, Actor>()
//	val hand: Hand = Hand(this)
//	private val handRegion: TextureRegion = game.skin.getRegion("hand")
	
	private var tick = 0F
	private val tickTime = 1/40F
	
	init
	{
//		cursorPosition = CursorPosition(user.uuid, 640, 360)
//		val gameMenuWindow = GameMenuWindow(this)
		
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

//		menuButton.addListener(ChangeListener(gameMenuWindow::show))
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

//		stage.addActor(new Debug(viewport, game.getShapeDrawer()));
//		if (Gdx.app.getType() != Application.ApplicationType.Desktop)
//		{
//			myCursor = Cursor(user, game.skin, true)
//			stage.addActor(myCursor)
//		} TODO add cursor
		if (!shader.isCompiled)
			ktx.log.error("RoomScreen | ERROR") { shader.log }
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
		
		val cursorBorder: TextureRegion = Scene2DSkin.defaultSkin["cursorborder"]
		val cursorBorderPixmap = cursorBorder.toPixmap()
		val cursorBase: TextureRegion = Scene2DSkin.defaultSkin["cursorbase"]
		val cursorBasePixmap = cursorBase.toPixmap()
		for (i in 0 until cursorBasePixmap.width)
		{
			for (j in 0 until cursorBasePixmap.height)
			{
				val color = Color(cursorBasePixmap.getPixel(i, j))
				cursorBasePixmap.setColor(color.mul(game.userColor))
				cursorBasePixmap.drawPixel(i, j)
			}
		}
		cursorBorderPixmap.drawPixmap(cursorBasePixmap, 0, 0)
		Gdx.graphics.setCursor(Gdx.graphics.newCursor(cursorBorderPixmap, 3, 0))
		cursorBorderPixmap.dispose()
		cursorBasePixmap.dispose()
	}
	
	private fun BitmapFont.chatTextWidth(message: String): Float
	{
		return min(textSize(message).x + 32, uiViewport.worldWidth - menuButton.width - 64)
	}
	
	override fun received(connection: Connection, `object`: Any)
	{
		when (`object`)
		{
			is UserJoinEvent ->
			{
				chat("${`object`.user.username} joined the game", Color.YELLOW)
				if (`object`.user != game.user && `object`.user !in state.users)
					addUser(`object`.user)
			}
			is Chat ->
			{
				chat(`object`.message, if (`object`.isSystemMessage) Color.YELLOW else null)
				info("Client | CHAT") { `object`.message }
			}
		}
	}
	
//	fun connectionClosed(connection: Connection?, e: Exception)
//	{
//		val menuScreen = MenuScreen(game)
//		game.setScreen(menuScreen)
//		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
//		if (!client.isDisconnectIntentional())
//		{
//			val errorMessage = e.toString() + if (e.cause != null) """
//
// 	${e.cause}
// 	""".trimIndent() else ""
//			menuScreen.connectingDialog.show(
//				"Disconnected",
//				if ("Connection reset" == e.message) "Server closed." else errorMessage,
//				"OK",
//				null
//			)
//		}
//	fun objectReceived(connection: Connection?, `object`: Serializable)
//	{
//		if (`object` is UserList)
//		{
//			val users: Array<User> = (`object` as UserList).users
//			for (user in users)
//			{
//				if (!user.equals(this.user)) addUser(user)
//			}
//		}
//		else if (`object` is UserEvent.UserLeaveEvent)
//		{
//			val event: UserEvent.UserLeaveEvent = `object` as UserEvent.UserLeaveEvent
//			addChatMessage(event.user.username.toString() + " left the game", Color.YELLOW)
//			removeUser(event.user)
//		}
//		else if (`object` is ServerObjectList)
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
//		else if (`object` is CursorPosition)
//		{
//			val cursorPosition: CursorPosition = `object` as CursorPosition
//			val user: User? = otherUsers.get<UUID>(cursorPosition.userUuid)
//			if (user != null) user.cursor.setTargetPosition(cursorPosition.getX() - 3, cursorPosition.getY() - 32)
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
	
	private fun addUser(user: User)
	{
		state.users += user
//		val cursor = Cursor(user, game.skin, false)
//		user.cursor = cursor
//		stage.addActor(cursor)
	}
	
//	private fun removeUser(user: User)
//	{
//		val removedUser: User = otherUsers.remove(user.uuid)!!
//		removedUser.cursor.remove()
//		Gdx.app.log("RoomScreen | INFO", "Removed " + user.username)
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
		
		tick += delta
		if (tick > tickTime)
		{
			tick = 0F
			tempVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
			stage.screenToStageCoordinates(tempVec)
//			if (cursorPosition.set(tempVec.x, tempVec.y))
//			{
//				client.send(cursorPosition)
//				if (myCursor != null) myCursor.setTargetPosition(cursorPosition.getX() - 3, cursorPosition.getY() - 32)
//				if (latestServerObjectPosition != null)
//				{
//					client.send(latestServerObjectPosition)
//					latestServerObjectPosition = null
//				}
//			}
		}
	}
	
	override fun dispose()
	{
		super.dispose()
		shader.dispose()
		chatHistory.clearChildren()
		Network.stop()
	}
}
