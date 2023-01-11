package misterbander.gframework.scene2d

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import misterbander.gframework.util.GdxStringBuilder
import misterbander.gframework.util.insertMany
import kotlin.math.max
import kotlin.math.min

/**
 * Extension of [TextField] that supports selection color.
 */
class GTextField(text: String, style: GTextFieldStyle) : GTextWidget<GTextField.GTextFieldStyle>(text)
{
	override var style = style
		set(value)
		{
			field = value
			textHeight = value.font.capHeight - value.font.descent*2
			updateDisplayText()
			invalidateHierarchy()
		}
	
	private val colorMarkupDisplayTextBuilder = GdxStringBuilder()
	private val strsToInsert = IntMap<String>()
	
	init
	{
		this.style = style
		addListener(inputListener)
		setSize(prefWidth, prefHeight)
	}
	
	override fun getTextY(font: BitmapFont, background: Drawable?): Float
	{
		val height = height
		var textY = textHeight/2 + font.descent
		textY = if (background != null)
		{
			val bottom = background.bottomHeight
			textY + (height - background.topHeight - bottom)/2 + bottom
		}
		else
			textY + height/2
		if (font.usesIntegerPositions())
			textY = textY.toInt().toFloat()
		return textY
	}
	
	override fun drawSelection(selection: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float) =
		selection.draw(
			batch, x + textOffset + selectionX + fontOffset, y - textHeight - font.descent, selectionWidth,
			textHeight
		)
	
	override fun drawText(batch: Batch, font: BitmapFont, x: Float, y: Float)
	{
		if (!hasKeyboardFocus() || !hasSelection || cursor == selectionStart)
		{
			font.draw(batch, displayText, x + textOffset, y, visibleTextStart, visibleTextEnd, 0F, Align.left, false)
			return
		}
		val prev = font.data.markupEnabled
		colorMarkupDisplayTextBuilder.append(displayText.subSequence(visibleTextStart, visibleTextEnd))
		
		// Escape all '['
		colorMarkupDisplayTextBuilder.forEachIndexed { index, c ->
			if (c == '[')
				strsToInsert.put(index, "[")
		}
		
		val highlightStart = max(min(cursor, selectionStart), visibleTextStart) - visibleTextStart
		val highlightEnd = min(max(cursor, selectionStart), visibleTextEnd) - visibleTextStart
		strsToInsert.put(
			highlightStart,
			"[#${if (style.selectionFontColor == null) style.fontColor else style.selectionFontColor}]${strsToInsert[highlightStart, ""]}"
		)
		strsToInsert.put(highlightEnd, "[]${strsToInsert[highlightEnd, ""]}")
		
		colorMarkupDisplayTextBuilder.insertMany(strsToInsert)
		strsToInsert.clear()
		
		font.data.markupEnabled = true
		font.draw(batch, colorMarkupDisplayTextBuilder.toStringAndClear(), x + textOffset, y, 0F, Align.left, false)
		font.data.markupEnabled = prev
	}
	
	override fun drawCursor(cursorPatch: Drawable, batch: Batch, font: BitmapFont, x: Float, y: Float)
	{
		cursorPatch.draw(
			batch,
			x + textOffset + glyphPositions[cursor] - glyphPositions[visibleTextStart] + fontOffset + font.data.cursorX,
			y - textHeight - font.descent, cursorPatch.minWidth, textHeight
		)
	}
	
	open class GTextFieldStyle() : GTextWidgetStyle()
	{
		var selectionFontColor: Color? = null
		
		constructor(style: GTextFieldStyle) : this()
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
			if (style.selectionFontColor != null)
				selectionFontColor = Color(style.selectionFontColor)
		}
	}
}
