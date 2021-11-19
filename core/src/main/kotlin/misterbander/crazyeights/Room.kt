package misterbander.crazyeights

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
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.esotericsoftware.kryonet.Connection
import ktx.actors.KtxInputListener
import ktx.actors.along
import ktx.actors.onChange
import ktx.actors.onKey
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.collections.*
import ktx.graphics.use
import ktx.log.info
import ktx.math.component1
import ktx.math.component2
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.model.Chat
import misterbander.crazyeights.model.CursorPosition
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
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.scene2d.Card
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.CardHolder
import misterbander.crazyeights.scene2d.CrazyEightsCursor
import misterbander.crazyeights.scene2d.Debug
import misterbander.crazyeights.scene2d.Gizmo
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.dialogs.GameMenuDialog
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.crazyeights.scene2d.transformToGroupCoordinates
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.gTextField
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.textSize
import misterbander.gframework.util.toPixmap
import kotlin.math.min

class Room(game: CrazyEights) : CrazyEightsScreen(game)
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
	val gizmo1 = Gizmo(game.shapeDrawer, Color.GREEN) // TODO ###### remove debug
	val gizmo2 = Gizmo(game.shapeDrawer, Color.CYAN)
	
	// Tabletop states
	val tabletop = Tabletop(this)
	private var cursorPosition = CursorPosition()
	
	// Networking
	var clientListener = ClientListener()
	var selfDisconnect = false
	private val syncServerAction: RepeatAction = forever(
		delay(1/40F, Actions.run {
			val (inputX, inputY) = stage.screenToStageCoordinates(tempVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()))
			if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
			{
				cursorPosition.x = inputX
				cursorPosition.y = inputY
				tabletop.myCursor?.setTargetPosition(cursorPosition.x, cursorPosition.y)
				game.client?.sendTCP(cursorPosition)
			}
			game.client?.flushOutgoingPacketBuffer()
		}) along Actions.run { clientListener.processPackets() }
	)
	
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
			row()
			actor(debugInfo).cell(colspan = 2, padLeft = 16F).inCell.left()
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
		stage += tabletop.cards
		stage += tabletop.hand
		stage += tabletop.cursors
		stage += gizmo1
		uiStage += gizmo2
		stage += Debug(this)
		
		stage += syncServerAction
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
		selfDisconnect = false
		tabletop.hand.arrange()
	}
	
	override fun render(delta: Float)
	{
		super.render(delta)
		if (transition.isRunning)
			tabletop.hand.reposition()
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
		
		tabletop.hand.reposition(width, height)
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
					chat("${user.username} joined the game", Color.YELLOW)
				}
				is UserLeftEvent ->
				{
					val user = packet.user
					tabletop -= user
					chat("${user.username} left the game", Color.YELLOW)
				}
				is Chat ->
				{
					val (_, message, isSystemMessage) = packet
					chat(message, if (isSystemMessage) Color.YELLOW else null)
					info("Client | CHAT") { message }
				}
				is CursorPosition ->
				{
					val (username, x, y) = packet
					val cursor: CrazyEightsCursor? = tabletop.userToCursorMap[username]
					if (cursor != tabletop.myCursor)
						cursor?.setTargetPosition(x, y)
					cursorPositionPool.free(packet)
				}
				is ObjectLockEvent -> // User attempts to lock an object
				{
					val (id, lockerUsername) = packet
					idToGObjectMap[id]!!.getModule<Lockable>()?.lock(tabletop.users[lockerUsername])
				}
				is ObjectUnlockEvent -> idToGObjectMap[packet.id].getModule<Lockable>()?.unlock()
				is ObjectOwnEvent ->
				{
					val (id, ownerUsername) = packet
					val toOwn = idToGObjectMap[id]!!
					toOwn.getModule<Lockable>()?.unlock()
					toOwn.isVisible = false
					tabletop.opponentHands.getOrPut(ownerUsername) { GdxArray() } += toOwn
				}
				is ObjectDisownEvent ->
				{
					val (id, x, y, rotation, isFaceUp, disownerUsername) = packet
					val toDisown = idToGObjectMap[id]!!
					toDisown.isVisible = true
					toDisown.getModule<SmoothMovable>()?.apply {
						setPositionAndTargetPosition(x, y)
						rotationInterpolator.set(rotation)
					}
					toDisown.getModule<Lockable>()?.lock(tabletop.users[disownerUsername])
					if (toDisown is Card)
						toDisown.isFaceUp = isFaceUp
					tabletop.opponentHands[disownerUsername].removeValue(toDisown, true)
				}
				is HandUpdateEvent ->
				{
					val hand = tabletop.opponentHands[packet.ownerUsername]
					for (gObject: GObject<CrazyEights> in hand)
					{
						if (gObject is CardGroup)
						{
							gObject.children.forEach { it.isVisible = false }
							gObject.dismantle()
						}
					}
				}
				is ObjectMoveEvent ->
				{
					val (id, x, y) = packet
					idToGObjectMap[id]!!.getModule<SmoothMovable>()?.apply { setTargetPosition(x, y) }
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
					val (id, cardIds) = packet
					val cards = GdxArray<Card>()
					cardIds.forEach { cards += idToGObjectMap[it] as Card }
					
					val firstX = cards[0].smoothMovable.xInterpolator.target
					val firstY = cards[0].smoothMovable.yInterpolator.target
					val firstRotation = cards[0].smoothMovable.rotationInterpolator.target
					val cardGroup = CardGroup(this@Room, id, firstX, firstY, firstRotation, GdxArray())
					tabletop.cards.addActorAfter(cards[0], cardGroup)
					cards.forEach { cardGroup += it }
					idToGObjectMap[id] = cardGroup
				}
				is CardGroupChangeEvent ->
				{
					val (cardIds, cardRotations, newCardGroupId, changerUsername) = packet
					if (changerUsername != game.user.username || newCardGroupId != -1)
					{
						val newCardGroup = if (newCardGroupId != -1) idToGObjectMap[newCardGroupId] as CardGroup else null
						for (i in cardIds.indices)
						{
							val card = idToGObjectMap[cardIds[i]] as Card
							card.cardGroup?.minusAssign(card)
							newCardGroup?.plusAssign(card)
							card.smoothMovable.rotationInterpolator.target = cardRotations[i]
						}
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
						0F, 0F, 0F,
						GdxArray()
					)
					idToGObjectMap[replacementCardGroupId] = replacementCardGroup
					cardHolder += replacementCardGroup
				}
				is CardGroupDismantleEvent -> (idToGObjectMap[packet.id] as CardGroup).dismantle()
			}
		}
	}
}
