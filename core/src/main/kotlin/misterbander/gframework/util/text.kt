package misterbander.gframework.util

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.StringBuilder
import ktx.collections.*

typealias GdxStringBuilder = StringBuilder

/**
 * Inserts multiple specified strings each at their specified offsets.
 * @param strs mapping of indices to strings to be inserted
 */
fun GdxStringBuilder.insertMany(strs: IntMap<String>)
{
	val offsets = obtain<GdxIntArray>()
	strs.keys().toArray(offsets)
	offsets.sort()
	offsets.reverse()
	for (i in 0 until offsets.size)
		insert(offsets[i], strs[offsets[i]])
	offsets.clear()
	Pools.free(offsets)
}

/**
 * @return A [Vector2] containing the width and height of the [text], in pixels. The returned [Vector2] is not safe for reuse.
 */
fun BitmapFont.textSize(text: String): Vector2
{
	val glyph = obtain<GlyphLayout>()
	glyph.setText(this, text)
	tempVec.set(glyph.width, glyph.height)
	Pools.free(glyph)
	return tempVec
}

/**
 * Wraps [text] to fit within [targetWidth] in pixels, adding line feeds between words where necessary.
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
 * Draws [text] centered at ([x], [y]).
 */
fun BitmapFont.drawCenter(batch: Batch, text: CharSequence, x: Float, y: Float)
{
	val textSize = textSize(text.toString())
	draw(batch, text, x - textSize.x/2, y + textSize.y/2, textSize.x, Align.center, false)
}
