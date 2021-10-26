package misterbander.gframework.util

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.StringBuilder as GdxStringBuilder

private val glyph = GlyphLayout()

/**
 * Returns the dimensions of a text in pixels based on the [BitmapFont].
 * @param text the text
 * @return A [Vector2] containing the dimensions of the text, in pixels. The returned [Vector2] is not safe for reuse.
 */
fun BitmapFont.textSize(text: String): Vector2
{
	glyph.setText(this, text)
	tempVec.set(glyph.width, glyph.height)
	return tempVec
}

/**
 * Wraps a string to fit within a specified width in pixels, adding line feeds between words where necessary.
 * @param text the text
 * @param targetWidth the width of the wrapped text
 * @return A string wrapped within the specified width.
 */
fun BitmapFont.wrap(text: String, targetWidth: Int): String
{
	val builder = GdxStringBuilder() // Current line builder
	var peeker = GdxStringBuilder() // Current line builder to check if the next word fits within the line
	val words = text.split(" ").toTypedArray()
	var isFirstWord = true
	// Add each word one by one, moving on to the next line if there's not enough space
	for (word in words)
	{
		peeker.append(if (isFirstWord) word else " $word") // Have the peeker check if the next word fits
		if (textSize(peeker.toString()).x <= targetWidth) // It fits
			builder.append(if (isFirstWord) word else " $word")
		else  // It doesn't fit, move on to the next line
		{
			builder.append("\n").append(word)
			peeker = GdxStringBuilder(word)
		}
		isFirstWord = false
	}
	return builder.toString()
}

/**
 * Draws a text using a [BitmapFont] centered at a specified position.
 * @param batch the [Batch] to draw with
 * @param str the text to draw
 * @param x the center x position
 * @param y the center y position
 */
fun BitmapFont.drawCenter(batch: Batch, str: CharSequence, x: Float, y: Float)
{
	val textSize = textSize(str.toString())
	draw(batch, str, x - textSize.x/2, y + textSize.y/2, textSize.x, Align.center, false)
}
