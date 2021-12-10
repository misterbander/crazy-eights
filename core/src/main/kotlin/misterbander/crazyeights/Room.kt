package misterbander.crazyeights

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryonet.Connection
import ktx.actors.KtxInputListener
import ktx.actors.onChange
import ktx.actors.plusAssign
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
import misterbander.crazyeights.model.GameState
import misterbander.crazyeights.net.BufferedListener
import misterbander.crazyeights.net.cursorPositionPool
import misterbander.crazyeights.net.packets.CardFlipEvent
import misterbander.crazyeights.net.packets.CardGroupChangeEvent
import misterbander.crazyeights.net.packets.CardGroupCreateEvent
import misterbander.crazyeights.net.packets.CardGroupDetachEvent
import misterbander.crazyeights.net.packets.CardGroupDismantleEvent
import misterbander.crazyeights.net.packets.CardSlideSoundEvent
import misterbander.crazyeights.net.packets.DrawTwosPlayedEvent
import misterbander.crazyeights.net.packets.EightsPlayedEvent
import misterbander.crazyeights.net.packets.HandUpdateEvent
import misterbander.crazyeights.net.packets.NewGameEvent
import misterbander.crazyeights.net.packets.ObjectDisownEvent
import misterbander.crazyeights.net.packets.ObjectLockEvent
import misterbander.crazyeights.net.packets.ObjectMoveEvent
import misterbander.crazyeights.net.packets.ObjectOwnEvent
import misterbander.crazyeights.net.packets.ObjectRotateEvent
import misterbander.crazyeights.net.packets.ObjectUnlockEvent
import misterbander.crazyeights.net.packets.ReversePlayedEvent
import misterbander.crazyeights.net.packets.SkipsPlayedEvent
import misterbander.crazyeights.net.packets.SuitDeclareEvent
import misterbander.crazyeights.net.packets.SwapSeatsEvent
import misterbander.crazyeights.net.packets.TouchUpEvent
import misterbander.crazyeights.net.packets.UserJoinedEvent
import misterbander.crazyeights.net.packets.UserLeftEvent
import misterbander.crazyeights.net.packets.onCardFlip
import misterbander.crazyeights.net.packets.onCardGroupChange
import misterbander.crazyeights.net.packets.onCardGroupCreate
import misterbander.crazyeights.net.packets.onCardGroupDetach
import misterbander.crazyeights.net.packets.onDrawTwosPlayed
import misterbander.crazyeights.net.packets.onEightsPlayed
import misterbander.crazyeights.net.packets.onNewGame
import misterbander.crazyeights.net.packets.onObjectDisown
import misterbander.crazyeights.net.packets.onObjectLock
import misterbander.crazyeights.net.packets.onObjectMove
import misterbander.crazyeights.net.packets.onObjectOwn
import misterbander.crazyeights.net.packets.onObjectRotate
import misterbander.crazyeights.net.packets.onReversePlayed
import misterbander.crazyeights.net.packets.onSkipsPlayed
import misterbander.crazyeights.net.packets.onSwapSeats
import misterbander.crazyeights.net.packets.onTouchUp
import misterbander.crazyeights.net.packets.onUserJoined
import misterbander.crazyeights.net.packets.onUserLeft
import misterbander.crazyeights.scene2d.CardGroup
import misterbander.crazyeights.scene2d.ChatBox
import misterbander.crazyeights.scene2d.CrazyEightsCursor
import misterbander.crazyeights.scene2d.Debug
import misterbander.crazyeights.scene2d.Gizmo
import misterbander.crazyeights.scene2d.OpponentHand
import misterbander.crazyeights.scene2d.Tabletop
import misterbander.crazyeights.scene2d.dialogs.GameMenuDialog
import misterbander.crazyeights.scene2d.dialogs.GameSettingsDialog
import misterbander.crazyeights.scene2d.dialogs.UserDialog
import misterbander.crazyeights.scene2d.modules.Lockable
import misterbander.crazyeights.scene2d.modules.SmoothMovable
import misterbander.gframework.layer.StageLayer
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.tempVec
import misterbander.gframework.util.toPixmap

class Room(game: CrazyEights) : CrazyEightsScreen(game)
{
	// Sounds
	val cardSlide = game.assetStorage[Sounds.cardSlide]
	val dramatic = game.assetStorage[Sounds.dramatic]
	val deepWhoosh = game.assetStorage[Sounds.deepwhoosh]
	
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
	var cameraAngle by cameraAngleInterpolator
	
	override val mainLayer by lazy {
		object : StageLayer(game, camera, viewport, false)
		{
			override fun postRender(delta: Float)
			{
				// TODO add proper ui for cam adjustments
				val dAngle = 45*delta
				if (Gdx.input.isKeyPressed(Input.Keys.LEFT_BRACKET))
					cameraAngle += dAngle
				if (Gdx.input.isKeyPressed(Input.Keys.RIGHT_BRACKET))
					cameraAngle -= dAngle
				
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
	
	val menuButton = scene2d.imageButton(MENU_BUTTON_STYLE) {
		onChange { click.play(); gameMenuDialog.show() }
	}
	val chatBox = ChatBox(this)
	val centerTitle = scene2d.label("Test", CENTER_TITLE_LABEL_STYLE)
	val centerTitleContainer = scene2d.container(centerTitle) {
		background = Scene2DSkin.defaultSkin["chatbackground"]
		isVisible = false
	}
	
	val debugInfo = scene2d.label("", INFO_LABEL_STYLE_XS)
	val gizmo1 = Gizmo(game.shapeDrawer, Color.RED) // TODO ###### remove debug
	val gizmo2 = Gizmo(game.shapeDrawer, Color.ORANGE)
	val gizmo3 = Gizmo(game.shapeDrawer, Color.YELLOW)
	val gizmo4 = Gizmo(game.shapeDrawer, Color.GREEN)
	val gizmo5 = Gizmo(game.shapeDrawer, Color.CYAN)
	
	private val uprightActors = GdxSet<Actor>()
	private val originalRotationMap = ObjectFloatMap<Actor>()
	
	val passButton = scene2d.textButton("Pass", TEXT_BUTTON_STYLE) {
		setOrigin(Align.center)
		isTransform = true
		isVisible = false
		addUprightGObject(this)
		onChange {
			click.play()
		}
	}
	
	// Tabletop states
	val tabletop = Tabletop(this)
	private val cursorPositions = IntMap<CursorPosition>()
	var gameState: GameState? = null
	val isGameStarted: Boolean
		get() = gameState != null
	
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
			stack {
				table {
					actor(menuButton).cell(pad = 16F).inCell.top()
					actor(chatBox).cell(expandX = true, fillX = true, maxHeight = 312F, pad = 16F)
					row()
					imageButton(SETTINGS_BUTTON_STYLE) {
						onChange {
							click.play()
							gameSettingsDialog.show()
						}
					}.cell(expand = true, colspan = 2, pad = 16F).inCell.bottom().right()
				}
				container(debugInfo) { top().left().padTop(128F).padLeft(16F) }
				table { actor(centerTitleContainer).cell(growX = true) }
			}.cell(grow = true)
		}
		
		stage += tabletop.playDirectionIndicator
		stage += tabletop.cardHolders
		stage += tabletop.opponentHands
		stage += tabletop.cards
		stage += tabletop.myHand
		stage += tabletop.powerCardEffects
		stage += tabletop.persistentPowerCardEffects
		stage += passButton
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
		tabletop.myHand.arrange()
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
					val cursorsMap: IntMap<CrazyEightsCursor>? = tabletop.userToCursorsMap[game.user.name]
					cursorsMap?.getOrPut(i) {
						val cursor = CrazyEightsCursor(this, game.user, true)
						cursorsMap[i] = cursor
						tabletop.cursors += cursor
						addUprightGObject(cursor)
						cursor
					}?.overwritePosition(inputX, inputY)
				}
				
				if (shouldSyncServer)
				{
					val cursorPosition = cursorPositions.getOrPut(i) { CursorPosition() }
					if (Vector2.dst2(inputX, inputY, cursorPosition.x, cursorPosition.y) > 1)
					{
						cursorPosition.apply {
							username = game.user.name
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
				tabletop.userToCursorsMap[game.user.name]?.remove(i)?.remove()
				game.client?.sendTCP(TouchUpEvent(game.user.name, i))
			}
		}
		if (shouldSyncServer)
			game.client?.flushOutgoingPacketBuffer()
		clientListener.processPackets()
		
//		if (Gdx.input.isKeyJustPressed(Input.Keys.E))
//			tabletop.persistentPowerCardEffects += EffectText(this, "+2", tabletop.userToHandMap.values().toArray().random())
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
	
	override fun resize(width: Int, height: Int)
	{
		super.resize(width, height)
		chatBox.resize()
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
			getModule<SmoothMovable>()!!.rotationInterpolator.overwrite(uprightAngle)
		else
			rotation = uprightAngle
	}
	
	override fun hide()
	{
		// Reset camera
		cameraAngleInterpolator.overwrite(0F)
		
		// Reset room state
		chatBox.clearChats()
		tabletop.reset()
		passButton.isVisible = false
		gameState = null
		centerTitleContainer.isVisible = false
		Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
	}
	
	inner class ClientListener : BufferedListener()
	{
		override fun connected(connection: Connection)
		{
			connection.setTimeout(0)
		}
		
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
				is UserJoinedEvent -> onUserJoined(packet)
				is UserLeftEvent -> onUserLeft(packet)
				is SwapSeatsEvent -> onSwapSeats(packet)
				is Chat ->
				{
					val (_, message, isSystemMessage) = packet
					chatBox.chat(message, if (isSystemMessage) Color.YELLOW else null)
					info("Client | CHAT") { message }
				}
				is CursorPosition ->
				{
					val (username, x, y, pointer) = packet
					val cursorsMap = tabletop.userToCursorsMap[username]!!
					cursorsMap[-1]?.remove()
					if (pointer in cursorsMap)
						cursorsMap[pointer]!!.setPosition(x, y)
					else
					{
						val cursor = CrazyEightsCursor(this@Room, tabletop.users[username]!!)
						cursorsMap[pointer] = cursor
						tabletop.cursors += cursor
						addUprightGObject(cursor)
						cursor.overwritePosition(x, y)
					}
					cursorPositionPool.free(packet)
				}
				is TouchUpEvent -> onTouchUp(packet)
				is ObjectLockEvent -> onObjectLock(packet) // User attempts to lock an object
				is ObjectUnlockEvent -> idToGObjectMap[packet.id]?.getModule<Lockable>()?.unlock()
				is ObjectOwnEvent -> onObjectOwn(packet)
				is ObjectDisownEvent -> onObjectDisown(packet)
				is HandUpdateEvent -> (tabletop.userToHandMap[packet.ownerUsername] as? OpponentHand)?.flatten()
				is ObjectMoveEvent -> onObjectMove(packet)
				is ObjectRotateEvent -> onObjectRotate(packet)
				is CardFlipEvent -> onCardFlip(packet)
				is CardGroupCreateEvent -> onCardGroupCreate(packet)
				is CardGroupChangeEvent -> onCardGroupChange(packet)
				is CardGroupDetachEvent -> onCardGroupDetach(packet)
				is CardGroupDismantleEvent -> (idToGObjectMap[packet.id] as CardGroup).dismantle()
//				is CardGroupShuffleEvent ->
//				{
//					val (id, seed) = packet
//					val cardGroup = idToGObjectMap[id] as CardGroup
//					cardGroup.shuffle(seed)
//				}
				is NewGameEvent -> onNewGame(packet)
				is GameState ->
				{
					gameState = packet
					if (packet.declaredSuit != null)
						tabletop.suitChooser!!.chosenSuit = packet.declaredSuit
					if (packet.players.size > 2)
					{
						if (packet.isPlayReversed)
							tabletop.playDirectionIndicator.scaleX = -1F
						tabletop.playDirectionIndicator += fadeIn(2F)
					}
				}
				is EightsPlayedEvent -> onEightsPlayed(packet)
				is SuitDeclareEvent -> tabletop.suitChooser?.chosenSuit = packet.suit
				is DrawTwosPlayedEvent -> onDrawTwosPlayed()
				is SkipsPlayedEvent -> onSkipsPlayed(packet)
				is ReversePlayedEvent -> onReversePlayed()
				is CardSlideSoundEvent -> cardSlide.play()
			}
		}
	}
}
