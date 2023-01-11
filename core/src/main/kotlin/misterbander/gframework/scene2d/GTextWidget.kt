package misterbander.gframework.scene2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Clipboard
import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.Timer
import ktx.collections.*
import ktx.math.vec2
import misterbander.gframework.util.GdxStringBuilder
import misterbander.gframework.util.obtain
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val BACKSPACE = 8.toChar()
const val CARRIAGE_RETURN = '\r'
const val NEWLINE = '\n'
const val TAB = '\t'
const val DELETE = 127.toChar()
const val BULLET = 149.toChar()

/**
 * `GTextWidget`s are [Widget]s that can hold editable text.
 */
abstract class GTextWidget<T : GTextWidget.GTextWidgetStyle>(text: String) : Widget(), Disableable
{
	private val tempVec1 = vec2()
	private val tempVec2 = vec2()
	private val tempVec3 = vec2()
	
	private var _text = text
	var text: String
		get() = _text
		set(value)
		{
			if (_text == value)
				return
			clearSelection()
			val oldText = _text
			_text = ""
			paste(value, false)
			if (programmaticChangeEvents)
				changeText(oldText, value)
			cursor = 0
		}
	protected var cursor = 0
	protected var selectionStart = 0
	protected var hasSelection = false
	protected open val writeEnters
		get() = false
	protected val layout = GlyphLayout()
	protected val glyphPositions = GdxFloatArray()
	
	/** The text widget's style. Modifying the returned style may not have an effect until style is set. */
	abstract var style: T
	/** The text that will be drawn in the text widget if no text has been entered. */
	var messageText: String? = null
	protected var displayText: CharSequence = ""
	var clipboard: Clipboard = Gdx.app.clipboard
	open val inputListener: InputListener = GTextWidgetClickListener()
	var listener: GTextWidgetListener? = null
	var filter: GTextWidgetFilter? = null
	var keyboard: TextField.OnscreenKeyboard = TextField.DefaultOnscreenKeyboard()
	/** If true (the default), tab/shift+tab will move to the next text widget. */
	var focusTraversal = true
	/**
	 * When false, [text] may contain characters not in the font, a space will be displayed instead.
	 *
	 * When true (the default), characters not in the font are stripped by setText. Characters not in the font are always stripped
	 * when typed or pasted.
	 */
	open val onlyFontChars: Boolean
		get() = true
	private var disabled = false
	/**
	 * Sets text horizontal alignment (left, center or right).
	 * @see Align
	 */
	var alignment = Align.left
	protected var selectionX = 0F
	protected var selectionWidth = 0F
	
	var undoText = ""
	var lastChangeTime = 0L
	
	/** If true, the text in this text widget will be shown as bullet characters. */
	var passwordMode = false
	private val passwordBuffer = GdxStringBuilder()
	/** The password character for the text widget. The character must be present in the [BitmapFont]. Default is 149 (bullet). */
	private var passwordCharacter = BULLET
		set(value)
		{
			field = value
			if (passwordMode)
				updateDisplayText()
		}
	
	protected var fontOffset = 0F
	protected var textHeight = 0F
	protected var textOffset = 0F
	var renderOffset = 0F
	protected var visibleTextStart = 0
	protected var visibleTextEnd = 0
	var maxLength = 0
	
	var focused = false
	var cursorOn = false
	var blinkTime = 0.32F
	val blinkTask = object : Timer.Task()
	{
		override fun run()
		{
			if (stage == null)
			{
				cancel()
				return
			}
			cursorOn = !cursorOn
			Gdx.graphics.requestRendering()
		}
	}
	
	val keyRepeatTask = KeyRepeatTask()
	/**
	 * If false, methods that change the text will not fire [ChangeEvent][ChangeListener.ChangeEvent], the event will be
	 * fired only when the user changes the text.
	 */
	var programmaticChangeEvents = false
	
	protected open fun letterUnderCursor(x: Float): Int
	{
		var lookingAtX = x
		lookingAtX -= textOffset + fontOffset - style.font.data.cursorX - glyphPositions[visibleTextStart]
		if (backgroundDrawable != null)
			lookingAtX -= style.background!!.leftWidth
		val n = glyphPositions.size
		val glyphPositions: FloatArray = glyphPositions.items
		for (i in 1 until n)
		{
			if (glyphPositions[i] > lookingAtX)
				return if (glyphPositions[i] - lookingAtX <= lookingAtX - glyphPositions[i - 1]) i else i - 1
		}
		return n - 1
	}
	
	protected open fun Char.isWordCharacter(): Boolean = isLetterOrDigit()
	
	protected open fun wordUnderCursor(at: Int): IntArray
	{
		var right = text.length
		var left = 0
		var index = at
		if (at >= text.length)
		{
			left = text.length
			right = 0
		}
		else
		{
			while (index < right)
			{
				if (!text[index].isWordCharacter())
				{
					right = index
					break
				}
				index++
			}
			index = at - 1
			while (index > -1)
			{
				if (!text[index].isWordCharacter())
				{
					left = index + 1
					break
				}
				index--
			}
		}
		return intArrayOf(left, right)
	}
	
	open fun wordUnderCursor(x: Float): IntArray = wordUnderCursor(letterUnderCursor(x))
	
	open fun withinMaxLength(size: Int): Boolean = maxLength <= 0 || size < maxLength
	
	protected open fun calculateOffsets()
	{
		var visibleWidth = width
		backgroundDrawable?.let { visibleWidth -= it.leftWidth + it.rightWidth }
		val glyphCount = glyphPositions.size
		val glyphPositions: FloatArray = glyphPositions.items
		
		// Check if the cursor has gone out the left or right side of the visible area and adjust renderOffset.
		cursor = MathUtils.clamp(cursor, 0, glyphCount - 1)
		val distance = glyphPositions[max(0, cursor - 1)] + renderOffset
		if (distance <= 0)
			renderOffset -= distance
		else
		{
			val index = min(glyphCount - 1, cursor + 1)
			val minX = glyphPositions[index] - visibleWidth
			if (-renderOffset < minX)
				renderOffset = -minX
		}
		
		// Prevent renderOffset from starting too close to the end, e.g. after text was deleted.
		var maxOffset = 0F
		val width = glyphPositions[glyphCount - 1]
		for (i in glyphCount - 2 downTo 0)
		{
			val x = glyphPositions[i]
			if (width - x > visibleWidth)
				break
			maxOffset = x
		}
		if (-renderOffset > maxOffset)
			renderOffset = -maxOffset
		
		// Calculate first visible char based on render offset
		visibleTextStart = 0
		var startX = 0F
		for (i in 0 until glyphCount)
		{
			if (glyphPositions[i] >= -renderOffset)
			{
				visibleTextStart = i
				startX = glyphPositions[i]
				break
			}
		}
		
		// Calculate last visible char based on visible width and render offset
		var end = visibleTextStart + 1
		val endX = visibleWidth - renderOffset
		val n = min(displayText.length, glyphCount)
		while (end <= n)
		{
			if (glyphPositions[end] > endX)
				break
			end++
		}
		visibleTextEnd = max(0, end - 1)
		if (alignment and Align.left == 0)
		{
			textOffset = visibleWidth - glyphPositions[visibleTextEnd] - fontOffset + startX
			if (alignment and Align.center != 0)
				textOffset = (textOffset*0.5F).roundToInt().toFloat()
		}
		else
			textOffset = startX + renderOffset
		
		// Calculate selection x position and width
		if (hasSelection)
		{
			val minIndex = min(cursor, selectionStart)
			val maxIndex = max(cursor, selectionStart)
			val minX = max(glyphPositions[minIndex] - glyphPositions[visibleTextStart], -textOffset)
			val maxX = min(glyphPositions[maxIndex] - glyphPositions[visibleTextStart], visibleWidth - textOffset)
			selectionX = minX
			selectionWidth = maxX - minX - style.font.data.cursorX
		}
	}
	
	protected open val backgroundDrawable: Drawable?
		get()
		{
			if (disabled && style.disabledBackground != null)
				return style.disabledBackground
			return if (style.focusedBackground != null && hasKeyboardFocus()) style.focusedBackground else style.background
		}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val focused = hasKeyboardFocus()
		if (focused != this.focused || focused && !blinkTask.isScheduled)
		{
			this.focused = focused
			blinkTask.cancel()
			cursorOn = focused
			if (focused)
				Timer.schedule(blinkTask, blinkTime, blinkTime)
			else
				keyRepeatTask.cancel()
		}
		else if (!focused)
			cursorOn = false
		val font = style.font
		val fontColor = if (disabled && style.disabledFontColor != null)
			style.disabledFontColor!!
		else if (focused && style.focusedFontColor != null)
			style.focusedFontColor!!
		else
			style.fontColor
		val selection = style.selection
		val cursorPatch = style.cursor
		val background = backgroundDrawable
		val color: Color = color
		val x = x
		val y = y
		val width = width
		val height = height
		batch.setColor(color.r, color.g, color.b, color.a*parentAlpha)
		var bgLeftWidth = 0F
		var bgRightWidth = 0F
		if (background != null)
		{
			background.draw(batch, x, y, width, height)
			bgLeftWidth = background.leftWidth
			bgRightWidth = background.rightWidth
		}
		val textY = getTextY(font, background)
		calculateOffsets()
		if (focused && hasSelection && selection != null)
			drawSelection(selection, batch, font, x + bgLeftWidth, y + textY)
		val yOffset = if (font.isFlipped) -textHeight else 0F
		if (displayText.isEmpty())
		{
			if ((!focused || disabled) && messageText != null)
			{
				val messageFont = style.messageFont ?: font
				if (style.messageFontColor != null)
				{
					messageFont.setColor(
						style.messageFontColor!!.r, style.messageFontColor!!.g, style.messageFontColor!!.b,
						style.messageFontColor!!.a*color.a*parentAlpha
					)
				}
				else
					messageFont.setColor(0.7F, 0.7F, 0.7F, color.a*parentAlpha)
				drawMessageText(
					batch,
					messageFont,
					x + bgLeftWidth,
					y + textY + yOffset,
					width - bgLeftWidth - bgRightWidth
				)
			}
		}
		else
		{
			font.setColor(fontColor.r, fontColor.g, fontColor.b, fontColor.a*color.a*parentAlpha)
			drawText(batch, font, x + bgLeftWidth, y + textY + yOffset)
		}
		if (!disabled && cursorOn && cursorPatch != null)
			drawCursor(cursorPatch, batch, font, x + bgLeftWidth, y + textY)
	}
	
	protected abstract fun getTextY(font: BitmapFont, background: Drawable?): Float
	
	/**
	 * Draws selection rectangle.
	 */
	protected abstract fun drawSelection(selection: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float)
	
	protected abstract fun drawText(batch: Batch, font: BitmapFont, x: Float, y: Float)
	
	protected open fun drawMessageText(batch: Batch, font: BitmapFont, x: Float, y: Float, maxWidth: Float)
	{
		font.draw(batch, messageText!!, x, y, 0, messageText!!.length, maxWidth, alignment, false, "...")
	}
	
	protected abstract fun drawCursor(cursorPatch: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float)
	
	open fun updateDisplayText()
	{
		val font = style.font
		val data: BitmapFont.BitmapFontData = font.data
		val text = text
		val buffer = StringBuilder()
		for (char in text)
			buffer.append(if (data.hasGlyph(char)) char else ' ')
		val newDisplayText = buffer.toString()
		displayText = if (passwordMode && data.hasGlyph(passwordCharacter))
		{
			if (passwordBuffer.length > text.length)
				passwordBuffer.setLength(text.length)
			else
			{
				for (i in passwordBuffer.length until text.length)
					passwordBuffer.append(passwordCharacter)
			}
			passwordBuffer
		}
		else
			newDisplayText
		layout.setText(font, displayText.toString().replace('\r', ' ').replace('\n', ' '))
		glyphPositions.clear()
		var x = 0F
		if (layout.runs.isNotEmpty())
		{
			val xAdvances: GdxFloatArray = layout.runs.first().xAdvances
			fontOffset = xAdvances.first()
			for (i in 1 until xAdvances.size)
			{
				glyphPositions.add(x)
				x += xAdvances[i]
			}
		}
		else
			fontOffset = 0F
		glyphPositions.add(x)
		visibleTextStart = min(visibleTextStart, glyphPositions.size - 1)
		visibleTextEnd = MathUtils.clamp(visibleTextEnd, visibleTextStart, glyphPositions.size - 1)
		if (selectionStart > newDisplayText.length)
			selectionStart = text.length
	}
	
	/**
	 * Copies the contents of this TextField to the [Clipboard] implementation set on this TextField.
	 */
	open fun copy()
	{
		if (hasSelection && !passwordMode)
			clipboard.contents = text.substring(min(cursor, selectionStart), max(cursor, selectionStart))
	}
	
	/**
	 * Copies the selected contents of this TextField to the [Clipboard] implementation set on this TextField, then removes
	 * it.
	 */
	open fun cut(fireChangeEvent: Boolean = programmaticChangeEvents)
	{
		if (hasSelection && !passwordMode)
		{
			copy()
			cursor = delete(fireChangeEvent)
			updateDisplayText()
		}
	}
	
	open fun paste(contentToPaste: String?, fireChangeEvent: Boolean)
	{
		var content = contentToPaste ?: return
		val buffer = StringBuilder()
		var textLength = text.length
		if (hasSelection)
			textLength -= abs(cursor - selectionStart)
		val data: BitmapFont.BitmapFontData = style.font.data
		var i = 0
		val n = content.length
		while (i < n)
		{
			if (!withinMaxLength(textLength + buffer.length))
				break
			val c = content[i]
			if (!(writeEnters && (c == NEWLINE || c == CARRIAGE_RETURN)))
			{
				if (c == '\r' || c == '\n')
				{
					i++
					continue
				}
				if (onlyFontChars && !data.hasGlyph(c))
				{
					i++
					continue
				}
				if (filter?.acceptChar(this, c) == false)
				{
					i++
					continue
				}
			}
			buffer.append(c)
			i++
		}
		content = buffer.toString()
		if (hasSelection)
			cursor = delete(fireChangeEvent)
		if (fireChangeEvent)
			changeText(text, insert(cursor, content, text))
		else
			_text = insert(cursor, content, text)
		updateDisplayText()
		cursor += content.length
	}
	
	open fun insert(position: Int, text: CharSequence, to: String): String =
		if (to.isEmpty()) text.toString() else to.substring(0, position) + text + to.substring(position, to.length)
	
	open fun delete(fireChangeEvent: Boolean): Int
	{
		val from = selectionStart
		val to = cursor
		val minIndex = min(from, to)
		val maxIndex = max(from, to)
		val newText = (if (minIndex > 0) text.substring(0, minIndex) else "") + if (maxIndex < text.length)
			text.substring(
				maxIndex,
				text.length
			)
		else
			""
		if (fireChangeEvent)
			changeText(text, newText)
		else
			_text = newText
		clearSelection()
		return minIndex
	}
	
	/**
	 * Sets the [keyboard focus][Stage.setKeyboardFocus] to the next TextField. If no next text widget is found, the
	 * onscreen keyboard is hidden. Does nothing if the text widget is not in a stage.
	 * @param up If true, the text widget with the same or next smallest y coordinate is found, else the next highest.
	 */
	open fun next(up: Boolean)
	{
		val stage = stage ?: return
		var current: GTextWidget<*> = this
		val currentCoords: Vector2 = current.parent.localToStageCoordinates(tempVec2.set(current.x, current.y))
		val bestCoords = tempVec1
		while (true)
		{
			var textField = current.findNextTextWidget(stage.actors, null, bestCoords, currentCoords, up)
			if (textField == null)
			{
				// Try to wrap around.
				if (up)
					currentCoords[-Float.MAX_VALUE] = -Float.MAX_VALUE
				else currentCoords[Float.MAX_VALUE] =
					Float.MAX_VALUE
				textField = current.findNextTextWidget(stage.actors, null, bestCoords, currentCoords, up)
			}
			if (textField == null)
			{
				Gdx.input.setOnscreenKeyboardVisible(false)
				break
			}
			if (stage.setKeyboardFocus(textField))
			{
				textField.selectAll()
				break
			}
			current = textField
			currentCoords.set(bestCoords)
		}
	}
	
	open fun findNextTextWidget(
		actors: GdxArray<Actor>,
		best: GTextWidget<*>?,
		bestCoords: Vector2,
		currentCoords: Vector2,
		up: Boolean
	): GTextWidget<*>?
	{
		var bestSoFar = best
		var i = 0
		while (i < actors.size)
		{
			val actor: Actor = actors[i]
			if (actor is GTextWidget<*>)
			{
				if (actor === this)
				{
					i++
					continue
				}
				if (actor.isDisabled || !actor.focusTraversal || !actor.ascendantsVisible())
				{
					i++
					continue
				}
				val actorCoords: Vector2 =
					actor.getParent().localToStageCoordinates(tempVec3.set(actor.getX(), actor.getY()))
				val below = actorCoords.y != currentCoords.y && (actorCoords.y < currentCoords.y) xor up
				val right = actorCoords.y == currentCoords.y && (actorCoords.x > currentCoords.x) xor up
				if (!below && !right)
				{
					i++
					continue
				}
				var better = bestSoFar == null || actorCoords.y != bestCoords.y && (actorCoords.y > bestCoords.y) xor up
				if (!better)
					better = actorCoords.y == bestCoords.y && (actorCoords.x < bestCoords.x) xor up
				if (better)
				{
					bestSoFar = actor
					bestCoords.set(actorCoords)
				}
			}
			else if (actor is Group)
				bestSoFar = findNextTextWidget(actor.children, bestSoFar, bestCoords, currentCoords, up)
			i++
		}
		return bestSoFar
	}
	
	open fun appendText(str: String)
	{
		clearSelection()
		cursor = text.length
		paste(str, programmaticChangeEvents)
	}
	
	/**
	 * @return True if the text was changed.
	 */
	open fun changeText(oldText: String, newText: String): Boolean
	{
		if (newText == oldText)
			return false
		_text = newText
		val changeEvent = obtain<ChangeListener.ChangeEvent>()
		val cancelled = fire(changeEvent)
		if (cancelled)
			_text = oldText
		Pools.free(changeEvent)
		return !cancelled
	}
	
	open val selection: String
		get() = if (hasSelection) text.substring(min(selectionStart, cursor), max(selectionStart, cursor)) else ""
	
	/** Sets the selected text.  */
	open fun setSelection(selectionStart: Int, selectionEnd: Int)
	{
		require(selectionStart >= 0) { "selectionStart must be >= 0" }
		require(selectionEnd >= 0) { "selectionEnd must be >= 0" }
		var selectionStartCapped = min(text.length, selectionStart)
		var selectionEndCapped = min(text.length, selectionEnd)
		if (selectionEndCapped == selectionStartCapped)
		{
			clearSelection()
			return
		}
		if (selectionEndCapped < selectionStartCapped)
		{
			val temp = selectionEndCapped
			selectionEndCapped = selectionStartCapped
			selectionStartCapped = temp
		}
		hasSelection = true
		this.selectionStart = selectionStartCapped
		cursor = selectionEndCapped
	}
	
	open fun selectAll() = setSelection(0, text.length)
	
	open fun clearSelection()
	{
		hasSelection = false
	}
	
	/**
	 * Sets the cursor position and clears any selection.
	 */
	open fun setCursorPosition(cursorPosition: Int)
	{
		require(cursorPosition >= 0) { "cursorPosition must be >= 0" }
		clearSelection()
		cursor = min(cursorPosition, text.length)
	}
	
	override fun getPrefWidth(): Float = 150F
	
	override fun getPrefHeight(): Float
	{
		var topAndBottom = 0F
		var minHeight = 0F
		if (style.background != null)
		{
			topAndBottom = max(topAndBottom, style.background!!.bottomHeight + style.background!!.topHeight)
			minHeight = max(minHeight, style.background!!.minHeight)
		}
		if (style.focusedBackground != null)
		{
			topAndBottom = max(
				topAndBottom,
				style.focusedBackground!!.bottomHeight + style.focusedBackground!!.topHeight
			)
			minHeight = max(minHeight, style.focusedBackground!!.minHeight)
		}
		if (style.disabledBackground != null)
		{
			topAndBottom = max(
				topAndBottom,
				style.disabledBackground!!.bottomHeight + style.disabledBackground!!.topHeight
			)
			minHeight = max(minHeight, style.disabledBackground!!.minHeight)
		}
		return max(topAndBottom + textHeight, minHeight)
	}
	
	override fun setDisabled(disabled: Boolean)
	{
		this.disabled = disabled
	}
	
	override fun isDisabled(): Boolean = disabled
	
	protected open fun moveCursor(forward: Boolean, jump: Boolean)
	{
		val limit = if (forward) text.length else 0
		val charOffset = if (forward) 0 else -1
		while ((if (forward) ++cursor < limit else --cursor > limit) && jump)
		{
			if (!continueCursor(cursor, charOffset))
				break
		}
	}
	
	protected open fun continueCursor(index: Int, offset: Int): Boolean = text[index + offset].isWordCharacter()
	
	inner class KeyRepeatTask : Timer.Task()
	{
		var keycode = 0
		override fun run()
		{
			if (stage == null)
			{
				cancel()
				return
			}
			inputListener.keyDown(null, keycode)
		}
	}
	
	/**
	 * Interface for listening to typed characters.
	 * @author mzechner
	 */
	interface GTextWidgetListener
	{
		fun keyTyped(textField: GTextWidget<*>, c: Char)
	}
	
	/**
	 * Interface for filtering characters entered into the text widget.
	 * @author mzechner
	 */
	interface GTextWidgetFilter
	{
		fun acceptChar(textField: GTextWidget<*>, c: Char): Boolean
		
		class DigitsOnlyFilter : GTextWidgetFilter
		{
			override fun acceptChar(textField: GTextWidget<*>, c: Char): Boolean = c.isDigit()
		}
	}
	
	/**
	 * Basic input listener for the text widget.
	 */
	open inner class GTextWidgetClickListener : ClickListener()
	{
		override fun clicked(event: InputEvent?, x: Float, y: Float)
		{
			when (tapCount%4)
			{
				0 -> clearSelection()
				2 ->
				{
					val array = wordUnderCursor(x)
					setSelection(array[0], array[1])
				}
				3 -> selectAll()
			}
		}
		
		override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
		{
			if (!super.touchDown(event, x, y, pointer, button))
				return false
			if (pointer == 0 && button != 0)
				return false
			if (disabled)
				return true
			setCursorPosition(x, y)
			selectionStart = cursor
			stage?.keyboardFocus = this@GTextWidget
			keyboard.show(true)
			hasSelection = true
			return true
		}
		
		override fun touchDragged(event: InputEvent?, x: Float, y: Float, pointer: Int)
		{
			super.touchDragged(event, x, y, pointer)
			setCursorPosition(x, y)
		}
		
		override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int)
		{
			if (selectionStart == cursor)
				hasSelection = false
			super.touchUp(event, x, y, pointer, button)
		}
		
		protected open fun setCursorPosition(x: Float, y: Float)
		{
			cursor = letterUnderCursor(x)
			cursorOn = focused
			blinkTask.cancel()
			if (focused)
				Timer.schedule(blinkTask, blinkTime, blinkTime)
		}
		
		protected open fun goHome(jump: Boolean)
		{
			cursor = 0
		}
		
		protected open fun goEnd(jump: Boolean)
		{
			cursor = text.length
		}
		
		override fun keyDown(event: InputEvent?, keycode: Int): Boolean
		{
			if (disabled)
				return false
			cursorOn = focused
			blinkTask.cancel()
			if (focused)
				Timer.schedule(blinkTask, blinkTime, blinkTime)
			if (!hasKeyboardFocus())
				return false
			var repeat = false
			val ctrl = UIUtils.ctrl()
			val jump = ctrl && !passwordMode
			var handled = true
			if (ctrl)
			{
				when (keycode)
				{
					Input.Keys.V ->
					{
						paste(clipboard.contents, true)
						repeat = true
					}
					Input.Keys.C, Input.Keys.INSERT ->
					{
						copy()
						return true
					}
					Input.Keys.X ->
					{
						cut(true)
						return true
					}
					Input.Keys.A ->
					{
						selectAll()
						return true
					}
					Input.Keys.Z ->
					{
						val oldText = text
						text = undoText
						undoText = oldText
						updateDisplayText()
						return true
					}
					else -> handled = false
				}
			}
			if (UIUtils.shift())
			{
				when (keycode)
				{
					Input.Keys.INSERT -> paste(clipboard.contents, true)
					Input.Keys.FORWARD_DEL -> cut(true)
				}
				val temp = cursor
				fun select()
				{
					if (!hasSelection)
					{
						selectionStart = temp
						hasSelection = true
					}
				}
				when (keycode)
				{
					Input.Keys.LEFT ->
					{
						moveCursor(false, jump)
						repeat = true
						handled = true
						select()
					}
					Input.Keys.RIGHT ->
					{
						moveCursor(true, jump)
						repeat = true
						handled = true
						select()
					}
					Input.Keys.HOME ->
					{
						goHome(jump)
						handled = true
						select()
					}
					Input.Keys.END ->
					{
						goEnd(jump)
						handled = true
						select()
					}
				}
			}
			else
			{
				// Cursor movement or other keys (kills selection).
				when (keycode)
				{
					Input.Keys.LEFT ->
					{
						moveCursor(false, jump)
						clearSelection()
						repeat = true
						handled = true
					}
					Input.Keys.RIGHT ->
					{
						moveCursor(true, jump)
						clearSelection()
						repeat = true
						handled = true
					}
					Input.Keys.HOME ->
					{
						goHome(jump)
						clearSelection()
						handled = true
					}
					Input.Keys.END ->
					{
						goEnd(jump)
						clearSelection()
						handled = true
					}
				}
			}
			cursor = MathUtils.clamp(cursor, 0, text.length)
			if (repeat)
				scheduleKeyRepeatTask(keycode)
			return handled
		}
		
		protected fun scheduleKeyRepeatTask(keycode: Int)
		{
			if (!keyRepeatTask.isScheduled || keyRepeatTask.keycode != keycode)
			{
				keyRepeatTask.keycode = keycode
				keyRepeatTask.cancel()
				Timer.schedule(keyRepeatTask, TextField.keyRepeatInitialTime, TextField.keyRepeatTime)
			}
		}
		
		override fun keyUp(event: InputEvent?, keycode: Int): Boolean
		{
			if (disabled)
				return false
			keyRepeatTask.cancel()
			return true
		}
		
		/**
		 * Checks if focus traversal should be triggered. The default implementation uses [TextField.focusTraversal] and the
		 * typed character, depending on the OS.
		 * @param character The character that triggered a possible focus traversal.
		 * @return true if the focus should change to the [next][TextField.next] input field.
		 */
		protected open fun checkFocusTraversal(character: Char): Boolean =
			focusTraversal && (character == TAB || (character == CARRIAGE_RETURN || character == NEWLINE) && (UIUtils.isAndroid || UIUtils.isIos))
		
		override fun keyTyped(event: InputEvent?, character: Char): Boolean
		{
			if (disabled)
				return false
			when (character)
			{
				BACKSPACE, TAB, NEWLINE, CARRIAGE_RETURN -> Unit
				else -> if (character.code < 32)
					return false
			}
			if (!hasKeyboardFocus())
				return false
			if (UIUtils.isMac && Gdx.input.isKeyPressed(Input.Keys.SYM))
				return true
			if (checkFocusTraversal(character))
				next(UIUtils.shift())
			else
			{
				val enter = character == CARRIAGE_RETURN || character == NEWLINE
				val delete = character == DELETE
				val backspace = character == BACKSPACE
				val add = if (enter) writeEnters else !onlyFontChars || style.font.data.hasGlyph(character)
				val remove = backspace || delete
				if (add || remove)
				{
					val oldText = text
					val oldCursor = cursor
					if (remove)
					{
						if (hasSelection)
							cursor = delete(false)
						else
						{
							if (backspace && cursor > 0)
							{
								_text = text.substring(0, cursor - 1) + text.substring(cursor--)
								renderOffset = 0F
							}
							if (delete && cursor < text.length)
								_text = text.substring(0, cursor) + text.substring(cursor + 1)
						}
					}
					if (add && !remove)
					{
						// Character may be added to the text.
						if (!enter && filter?.acceptChar(this@GTextWidget, character) == false)
							return true
						if (!withinMaxLength(text.length - if (hasSelection) abs(cursor - selectionStart) else 0))
							return true
						if (hasSelection)
							cursor = delete(false)
						val insertion = if (enter) "\n" else character.toString()
						_text = insert(cursor++, insertion, text)
					}
					if (changeText(oldText, text))
					{
						val time = System.currentTimeMillis()
						if (time - 750 > lastChangeTime)
							undoText = oldText
						lastChangeTime = time
						updateDisplayText()
					}
					else
						cursor = oldCursor
				}
			}
			listener?.keyTyped(this@GTextWidget, character)
			return true
		}
	}
	
	open class GTextWidgetStyle()
	{
		lateinit var font: BitmapFont
		var fontColor: Color = Color()
		var focusedFontColor: Color? = null
		var disabledFontColor: Color? = null
		var background: Drawable? = null
		var focusedBackground: Drawable? = null
		var disabledBackground: Drawable? = null
		var cursor: Drawable? = null
		var selection: Drawable? = null
		var messageFont: BitmapFont? = null
		var messageFontColor: Color? = null
		
		constructor(style: GTextWidgetStyle) : this()
		{
			font = style.font
			fontColor = Color(style.fontColor)
			if (style.focusedFontColor != null)
				focusedFontColor = Color(style.focusedFontColor)
			if (style.disabledFontColor != null)
				disabledFontColor = Color(style.disabledFontColor)
			
			background = style.background
			focusedBackground = style.focusedBackground
			disabledBackground = style.disabledBackground
			cursor = style.cursor
			selection = style.selection
			
			messageFont = style.messageFont
			if (style.messageFontColor != null)
				messageFontColor = Color(style.messageFontColor)
		}
	}
}
