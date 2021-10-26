package misterbander.gframework.scene2d

import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import ktx.collections.*

/**
 * [Actor] that draws an animation. The animation is drawn with its bottom left corner at the position of this actor.
 *
 * All rotations and scaling done on the actor are also applied to the animation. Rotations and scaling are applied relative
 * to the origin of the actor, which defaults to the center of the animation.
 */
class GAnimation(
	animationSheet: TextureRegion,
	frameColumns: Int,
	frameRows: Int = 1,
	frameDuration: Float = 1/30F,
	playMode: Animation.PlayMode = Animation.PlayMode.LOOP
) : Actor()
{
	val animation: Animation<TextureRegion>
	var stateTime = 0F
	var isPaused = false
	
	init
	{
		val frameWidth = animationSheet.regionWidth/frameColumns
		val frameHeight = animationSheet.regionHeight/frameRows
		animation = Animation(frameDuration, createFrames(animationSheet, frameWidth, frameHeight), playMode)
		setSize(frameWidth.toFloat(), frameHeight.toFloat())
		originX = width/2
		originY = height/2
	}
	
	private fun createFrames(animationSheet: TextureRegion, frameWidth: Int, frameHeight: Int): GdxArray<TextureRegion>
	{
		val frames: Array<Array<TextureRegion>> = animationSheet.split(frameWidth, frameHeight)
		return frames.flatten().toGdxArray()
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		if (!isPaused)
			stateTime += delta
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.color = color
		batch.draw(animation.getKeyFrame(stateTime), x, y, originX, originY, width, height, scaleX, scaleY, rotation)
	}
}
