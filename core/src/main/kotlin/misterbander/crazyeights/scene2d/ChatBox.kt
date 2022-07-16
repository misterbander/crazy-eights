package misterbander.crazyeights.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import ktx.actors.onKey
import ktx.actors.onKeyDown
import ktx.actors.onKeyboardFocus
import ktx.actors.onTouchDown
import ktx.actors.plusAssign
import ktx.actors.then
import ktx.app.Platform
import ktx.scene2d.*
import ktx.style.*
import misterbander.crazyeights.CHAT_TEXT_FIELD_STYLE
import misterbander.crazyeights.CrazyEights
import misterbander.crazyeights.LABEL_SMALL_STYLE
import misterbander.crazyeights.RoomScreen
import misterbander.crazyeights.model.Chat
import misterbander.gframework.scene2d.gTextField
import misterbander.gframework.util.textSize
import kotlin.math.min

class ChatBox(private val room: RoomScreen) : Table()
{
	private val game: CrazyEights
		get() = room.game
	private val uiStage: Stage
		get() = room.uiStage
	
	private val chatTextField = scene2d.gTextField(null, "", CHAT_TEXT_FIELD_STYLE) {
		messageText = if (Platform.isMobile) "Tap here to chat..." else "Press T to chat..."
		maxLength = 256
		setFocusTraversal(false)
		onKey { character ->
			if ((character == '\r' || character == '\n') && text.isNotEmpty())
			{
				game.client?.sendTCP(Chat(game.user, "<${game.user.name}> $text"))
				text = ""
				uiStage.keyboardFocus = room.fallbackActor
				uiStage.scrollFocus = null
			}
		}
		onKeyboardFocus { focused ->
			chatPopup.isVisible = !focused
			chatHistoryScrollPane.isVisible = focused
			uiStage.scrollFocus = if (focused) chatHistoryScrollPane else null
			Gdx.input.setOnscreenKeyboardVisible(focused)
		}
		onKeyDown { keyCode ->
			if (keyCode == Input.Keys.ESCAPE || keyCode == Input.Keys.BACK)
				setFocused(false)
		}
	}
	private val chatHistory = scene2d.verticalGroup {
		grow()
		onTouchDown { Gdx.input.setOnscreenKeyboardVisible(false) }
	}
	private val chatHistoryScrollPane = scene2d.scrollPane {
		actor = chatHistory
		isVisible = false
	}
	private val chatPopup = scene2d.verticalGroup { columnAlign(Align.left) }
	
	init
	{
		defaults().growX()
		add(chatTextField)
		row()
		add(scene2d.stack {
			container(chatPopup).top().left()
			actor(chatHistoryScrollPane)
		}).left()
	}
	
	fun setFocused(isFocused: Boolean)
	{
		if (isFocused)
		{
			uiStage.keyboardFocus = chatTextField
			uiStage.scrollFocus = chatHistoryScrollPane
		}
		else
		{
			uiStage.keyboardFocus = room.fallbackActor
			uiStage.scrollFocus = null
		}
	}
	
	@Suppress("UNCHECKED_CAST")
	fun resize()
	{
		for (actor: Actor in chatPopup.children)
		{
			val chatLabelContainer = actor as Container<Label>
			val label = chatLabelContainer.actor!!
			chatLabelContainer.width(label.style.font.chatTextWidth(label.text.toString()))
			chatLabelContainer.invalidateHierarchy()
		}
	}
	
	/**
	 * Appends a chat message to the chat history, and adds a chat label that disappears after 5 seconds.
	 * @param message the message
	 * @param color   color of the chat message
	 */
	fun chat(message: String, color: Color? = null)
	{
		val chatLabel = scene2d.label(message, LABEL_SMALL_STYLE) {
			wrap = true
			if (color != null)
				this.color = color.cpy()
		}
		chatPopup += scene2d.container(chatLabel) {
			background = Scene2DSkin.defaultSkin["background"]
			width(chatLabel.style.font.chatTextWidth(message))
			pad(4F, 16F, 4F, 16F)
			this += delay(10F) then alpha(0F, 1F) then Actions.removeActor(this)
		}
		
		if (chatPopup.children.size == 7) // Maximum 6 children
		{
			val firstChatPopup: Actor = chatPopup.removeActorAt(0, false)
			firstChatPopup.clear()
		}
		
		// Add to history
		val chatHistoryLabel = scene2d.label(message, LABEL_SMALL_STYLE) {
			wrap = true
			if (color != null)
				this.color = color.cpy()
		}
		chatHistory.pad(4F, 16F, 4F, 16F).space(8F)
		chatHistory += chatHistoryLabel
		chatHistoryScrollPane.validate()
		chatHistoryScrollPane.validate()
		chatHistoryScrollPane.scrollPercentY = 1F
	}
	
	private fun BitmapFont.chatTextWidth(message: String): Float =
		min(textSize(message).x + 32, room.uiViewport.worldWidth - room.menuButton.width - 64)
	
	fun clearChats()
	{
		chatPopup.clearChildren()
		chatHistory.clearChildren()
	}
}
