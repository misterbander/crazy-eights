package misterbander.gframework.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor

/**
 * Actor that draws a still image sprite. The sprite is drawn with its bottom left corner at the position of this actor.
 *
 * All rotations and scaling done on the actor are also applied to the sprite. Rotations and scaling are applied relative
 * to the origin of the actor, which defaults to the center of the sprite.
 */
class GSprite(private val region: TextureRegion) : Actor()
{
	init
	{
		setSize(region.regionWidth.toFloat(), region.regionHeight.toFloat())
		originX = width/2
		originY = height/2
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.color = color
		batch.draw(region, x, y, originX, originY, width, height, scaleX, scaleY, rotation)
	}
}
