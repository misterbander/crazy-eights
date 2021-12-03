package misterbander.crazyeights

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.OrthographicCamera
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
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryonet.Connection
import ktx.actors.KtxInputListener
import ktx.actors.onChange
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.app.Platform
import ktx.collections.*
import ktx.graphics.use
import ktx.log.info
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
import misterbander.crazyeights.model.ServerCardGroup
import misterbander.crazyeights.net.BufferedListener
import misterbander.crazyeights.net.cursorPositionPool
import misterbander.crazyeights.net.objectMoveEventPool
import misterbander.crazyeights.net.objectRotateEventPool
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.CardHolder
import misterbander.crazyeights.scene2d.CrazyEightsCursor
import misterbander.crazyeights.scene2d.Debug
import misterbander.crazyeights.scene2d.Gizmo
import misterbander.crazyeights.scene2d.Groupable
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.dialogs.GameMenuDialog
import misterbander.crazyeights.scene2d.dialogs.GameSettingsDialog
import misterbander.crazyeights.scene2d.dialogs.UserDialog
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.crazyeights.scene2d.transformToGroupCoordinates
import misterbander.gframework.layer.StageLayer
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.gTextField
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.textSize
import misterbander.gframework.util.toPixmap
import kotlin.math.min

class Room(game: CrazyEights) : CrazyEightsScreen(game)
{
	// Shaders
	val brightenShader = game.assetStorage[Shaders.brighten]
	private val vignetteShader = game.assetStorage[Shaders.vignette]
	
	// Camera and layers
	val cameraAngleInterpolator = object : SmoothAngleInterpolator(0F)
	{
		override var value: Float = 0F
		private var prevCameraAngle = 0F
		
		override fun lerp(delta: Float)
		{
			super.lerp(delta)
			(camera as OrthographicCamera).rotate(-prevCameraAngle + value)
			prevCameraAngle = value
			
			uprightActors.forEach { it.makeUpright() }
		}
	}
	
	override val mainLayer by lazy {
		object : StageLayer(game, camera, viewport, false)
		{
			override fun postRender(delta: Float)
			{
				// TODO add proper ui for cam adjustments
				val dAngle = 45*delta
				if (Gdx.input.isKeyPressed(Input.Keys.LEFT_BRACKET))
					cameraAngleInterpolator.target += dAngle
				if (Gdx.input.isKeyPressed(Input.Keys.RIGHT_BRACKET))
					cameraAngleInterpolator.target -= dAngle
				
				cameraAngleInterpolator.lerp(delta)
				camera.update()
				super.postRender(delta)
			}
		}
	}
	
	// UI
	private val gameMenuDialog = GameMenuDialog(this)
	val userDialog = UserDialog(this)
	val gameSettingsDialog = GameSettingsDialog(this)
	
	private val menuButton = scene2d.imageButton(MENU_BUTTON_STYLE) {
		onChange { click.play(); gameMenuDialog.show() }
	}
	private val chatTextField = scene2d.gTextField(null, "", CHAT_TEXT_FIELD_STYLE) {
		messageText = if (Platform.isMobile) "Tap here to chat..." else "Press T to chat..."
		maxLength = 256
		setFocusTraversal(false)
		onKey { character ->
			if ((character == '\r' || character == '\n') && text.isNotEmpty())
			{
				game.client?.sendTCP(Chat(game.user, "<${game.user.username}> $text"))
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
	val debugInfo = scene2d.label("", INFO_LABEL_STYLE_XS)
	val gizmo1 = Gizmo(game.shapeDrawer, Color.RED) // TODO ###### remove debug
	val gizmo2 = Gizmo(game.shapeDrawer, Color.ORANGE)
	val gizmo3 = Gizmo(game.shapeDrawer, Color.YELLOW)
	val gizmo4 = Gizmo(game.shapeDrawer, Color.GREEN)
	val gizmo5 = Gizmo(game.shapeDrawer, Color.CYAN)
	
	private val uprightActors = GdxSet<Actor>()
	private val originalRotationMap = ObjectFloatMap<Actor>()
	
	// Tabletop states
	val tabletop = Tabletop(this)
	private val cursorPositions = IntMap<CursorPosition>()
	
	// Networking
	var clientListener = ClientListener()
	var selfDisconnect = false
	var timeSinceLastSync = 0F
	
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
			}.cell(expandX = true, fillX = true, maxHeight = 312F, pad = 16F)
			row()
			actor(debugInfo).cell(colspan = 2, padLeft = 16F).inCell.left()
			row()
			imageButton(SETTINGS_BUTTON_STYLE) {
				onChange {
					click.play()
					gameSettingsDialog.show()
				}
			}.cell(expand = true, colspan = 2, pad = 16F).inCell.bottom().right()
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
		stage += tabletop.cardHolders
		stage += tabletop.opponentHands
		stage += tabletop.cards
		stage += tabletop.hand
		stage += tabletop.cursors
		stage += tabletop.myCursors
		stage += gizmo1
		stage += gizmo2
		stage += gizmo3
		stage += gizmo4
		stage += gizmo5
		stage += Debug(this)
	}
	
	override fun show()
	{
		super.show()
		if (Platform.isDesktop)
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
		
		selfDisconnect = false
		tabletop.hand.arrange()
	}
	
	override fun render(delta: Float)
	{
		super.render(delta)
		timeSinceLastSync += delta
		val shouldSyncServer = if (timeSinceLastSync > 1/40F)
		{
			timeSinceLastSync = 0F
			true
		}
		else
			false
		for (i in 0..19)
		{
			if (Gdx.input.isTouched(i) || Platform.isDesktop && i == 0)
			{
				val (inputX, inputY) = stage.screenToStageCoordinates(
					tempVec.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
				)
				if (!Platform.isDesktop || i != 0)
				{
					val cursorsMap = tabletop.userToCursorsMap[game.user.username]!!
					cursorsMap.getOrPut(i) {
						val cursor = CrazyEightsCursor(this, game.user, true)
						cursorsMap[i] = cursor
						tabletop.cursors += cursor
						addUprightGObject(cursor)
						cursor
					}.setPositionAndTargetPosition(inputX, inputY)
				}
				
				if (shouldSyncServer)
				{
					val cursorPosition = cursorPositions.getOrPut(i) { CursorPosition() }
					if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
					{
						cursorPosition.apply {
							username = game.user.username
							x = inputX
							y = inputY
							pointer = i
						}
						game.client?.sendTCP(cursorPosition)
					}
				}
			}
			else if (i in cursorPositions && cursorPositions.size > 1)
			{
				cursorPositions.remove(i)
				tabletop.userToCursorsMap[game.user.username]!!.remove(i)?.remove()
				game.client?.sendTCP(TouchUpEvent(game.user.username, i))
			}
		}
		if (shouldSyncServer)
			game.client?.flushOutgoingPacketBuffer()
		clientListener.processPackets()
	}
	
	override fun clearScreen()
	{
		game.batch.use {
			it.shader = vignetteShader
			game.shapeDrawer.setColor(BACKGROUND_COLOR)
			game.shapeDrawer.filledRectangle(0F, 0F, viewport.worldWidth, viewport.worldHeight)
			it.shader = null
			it.color = Color.WHITE
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun resize(width: Int, height: Int)
	{
		super.resize(width, height)
		for (actor: Actor in chatPopup.children)
		{
			val chatLabelContainer = actor as Container<Label>
			val label = chatLabelContainer.actor!!
			chatLabelContainer.width(label.style.font.chatTextWidth(label.text.toString()))
			chatLabelContainer.invalidateHierarchy()
		}
		vignetteShader.bind()
		vignetteShader.setUniformf("u_resolution", width.toFloat(), height.toFloat())
	}
	
	fun addUprightGObject(actor: Actor)
	{
		uprightActors += actor
		originalRotationMap.put(actor, actor.rotation)
		actor.makeUpright()
	}
	
	private fun Actor.makeUpright()
	{
		val uprightAngle = -cameraAngleInterpolator.value + originalRotationMap[this, 0F]
		if (this is GObject<*> && hasModule<SmoothMovable>())
			getModule<SmoothMovable>()!!.rotationInterpolator.set(uprightAngle)
		else
			rotation = uprightAngle
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
		// Reset camera
		cameraAngleInterpolator.set(0F)
		
		// Reset room state
		chatPopup.clearChildren()
		chatHistory.clearChildren()
		tabletop.reset()
		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
	}
	
	inner class ClientListener : BufferedListener()
	{
		override fun disconnected(connection: Connection)
		{
			val mainMenu = game.getScreen<MainMenu>()
			if (!selfDisconnect)
			{
				mainMenu.messageDialog.show("Disconnected", "Server closed.", "OK")
				game.network.stop()
			}
			transition.start(targetScreen = mainMenu)
		}
		
		@Suppress("UNCHECKED_CAST")
		override fun processPacket(packet: Any)
		{
			val idToGObjectMap = tabletop.idToGObjectMap
			when (packet)
			{
				is UserJoinedEvent ->
				{
					val user = packet.user
					if (user != game.user)
						tabletop += user
					if (!user.isAi)
						chat("${user.username} joined the game", Color.YELLOW)
					tabletop.arrangePlayers()
				}
				is UserLeftEvent ->
				{
					val user = packet.user
					tabletop -= user
					if (!user.isAi)
						chat("${user.username} left the game", Color.YELLOW)
					tabletop.arrangePlayers()
					if (user == userDialog.user)
						userDialog.hide()
				}
				is SwapSeatsEvent ->
				{
					val (user1, user2) = packet
					val keys: GdxArray<String> = tabletop.users.orderedKeys()
					val index1 = keys.indexOf(user1, false)
					val index2 = keys.indexOf(user2, false)
					keys.swap(index1, index2)
					tabletop.arrangePlayers()
				}
				is Chat ->
				{
					val (_, message, isSystemMessage) = packet
					chat(message, if (isSystemMessage) Color.YELLOW else null)
					info("Client | CHAT") { message }
				}
				is CursorPosition ->
				{
					val (username, x, y, pointer) = packet
					val cursorsMap = tabletop.userToCursorsMap[username]!!
					cursorsMap[-1]?.remove()
					if (pointer in cursorsMap)
						cursorsMap[pointer]!!.setTargetPosition(x, y)
					else
					{
						val cursor = CrazyEightsCursor(this@Room, tabletop.users[username]!!)
						cursorsMap[pointer] = cursor
						tabletop.cursors += cursor
						addUprightGObject(cursor)
						cursor.setPositionAndTargetPosition(x, y)
					}
					cursorPositionPool.free(packet)
				}
				is TouchUpEvent ->
				{
					val (username, pointer) = packet
					tabletop.userToCursorsMap[username].remove(pointer)?.remove()
				}
				is ObjectLockEvent -> // User attempts to lock an object
				{
					val (id, lockerUsername) = packet
					idToGObjectMap[id]!!.getModule<Lockable>()?.lock(tabletop.users[lockerUsername]!!)
				}
				is ObjectUnlockEvent -> idToGObjectMap[packet.id]!!.getModule<Lockable>()?.unlock()
				is ObjectOwnEvent ->
				{
					val (id, ownerUsername) = packet
					val toOwn = idToGObjectMap[id] as Groupable<CardGroup>
					val hand = tabletop.userToOpponentHandMap[ownerUsername]!!
					toOwn.getModule<Lockable>()?.unlock()
					hand += toOwn
					hand.arrange()
				}
				is ObjectDisownEvent ->
				{
					val (id, x, y, rotation, isFaceUp, disownerUsername) = packet
					val toDisown = idToGObjectMap[id] as Groupable<CardGroup>
					val hand = tabletop.userToOpponentHandMap[disownerUsername]!!
					hand -= toDisown
					hand.arrange()
					toDisown.getModule<SmoothMovable>()?.apply {
						setTargetPosition(x, y)
						rotationInterpolator.target = rotation
					}
					toDisown.getModule<Lockable>()?.lock(tabletop.users[disownerUsername]!!)
					if (toDisown is Card)
						toDisown.isFaceUp = isFaceUp
				}
				is HandUpdateEvent -> tabletop.userToOpponentHandMap[packet.ownerUsername]!!.flatten()
				is ObjectMoveEvent ->
				{
					val (id, x, y) = packet
					val toMove = idToGObjectMap[id]!!
					toMove.getModule<SmoothMovable>()?.setTargetPosition(x, y)
					if (toMove is CardGroup && toMove.type == ServerCardGroup.Type.PILE)
					{
						toMove.type = ServerCardGroup.Type.STACK
						toMove.arrange()
					}
					objectMoveEventPool.free(packet)
				}
				is ObjectRotateEvent ->
				{
					val (id, rotation) = packet
					idToGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { rotationInterpolator.target = rotation }
					objectRotateEventPool.free(packet)
				}
				is CardFlipEvent ->
				{
					val card = idToGObjectMap[packet.id] as Card
					card.isFaceUp = !card.isFaceUp
				}
				is CardGroupCreateEvent ->
				{
					val (id, serverCards) = packet
					val cards = serverCards.map { idToGObjectMap[it.id] as Card }
					val firstX = cards.first().smoothMovable.xInterpolator.target
					val firstY = cards.first().smoothMovable.yInterpolator.target
					val firstRotation = cards.first().smoothMovable.rotationInterpolator.target
					val cardGroup = CardGroup(this@Room, id, firstX, firstY, firstRotation)
					tabletop.cards.addActorAfter(cards.first(), cardGroup)
					cards.forEachIndexed { index, card: Card ->
						val (_, x, y, rotation) = serverCards[index]
						cardGroup += card
						card.smoothMovable.setTargetPosition(x, y)
						card.smoothMovable.rotationInterpolator.target = rotation
					}
					cardGroup.arrange()
					idToGObjectMap[id] = cardGroup
				}
				is CardGroupChangeEvent ->
				{
					val (cards, newCardGroupId, changerUsername) = packet
					if (changerUsername != game.user.username || newCardGroupId != -1)
					{
						val newCardGroup = if (newCardGroupId != -1) idToGObjectMap[newCardGroupId] as CardGroup else null
						for ((id, x, y, rotation) in cards)
						{
							val card = idToGObjectMap[id] as Card
							card.cardGroup?.minusAssign(card)
							newCardGroup?.plusAssign(card)
							card.smoothMovable.setTargetPosition(x, y)
							card.smoothMovable.rotationInterpolator.target = rotation
						}
						newCardGroup?.arrange()
					}
				}
				is CardGroupDetachEvent ->
				{
					val (cardHolderId, replacementCardGroupId, changerUsername) = packet
					val cardHolder = idToGObjectMap[cardHolderId] as CardHolder
					if (changerUsername != game.user.username)
					{
						val cardGroup = cardHolder.cardGroup!!
						cardGroup.transformToGroupCoordinates(tabletop.cards)
						tabletop.cards += cardGroup
					}
					val replacementCardGroup = CardGroup(
						this@Room,
						replacementCardGroupId,
						type = cardHolder.defaultType
					)
					idToGObjectMap[replacementCardGroupId] = replacementCardGroup
					cardHolder += replacementCardGroup
				}
				is CardGroupDismantleEvent -> (idToGObjectMap[packet.id] as CardGroup).dismantle()
			}
		}
	}
}
