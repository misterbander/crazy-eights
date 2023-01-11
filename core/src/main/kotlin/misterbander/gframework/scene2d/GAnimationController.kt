package misterbander.gframework.scene2d

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import kotlin.math.max

/**
 * Often an object has more than one animation, this [Actor] stores a list of [GAnimation]s and can be used to switch
 * to the correct animation when desired.
 *
 * When changing the animation, the other [GAnimation]s will be reset to their first frame.
 *
 * The size of this actor will be determined by the maximum size of the given [GAnimation]s. All rotations and scaling
 * done on the actor are also applied to the animation. Rotations and scaling are applied relative to the origin of the
 * actor, which defaults to the center of the animation.
 */
class GAnimationController(private vararg val animations: GAnimation) : Group()
{
	var animation: GAnimation = animations[0]
		set(value)
		{
			if (field == value)
				return
			if (value !in animations)
				throw IllegalArgumentException("animation not in animations list")
			field = value
			for (animation in animations)
			{
				if (animation != value)
					animation.stateTime = 0F
			}
		}
	
	init
	{
		var width = 0F
		var height = 0F
		for (animation in animations)
		{
			width = max(animation.width, width)
			height = max(animation.height, height)
		}
		setSize(width, height)
		originX = width/2
		originY = height/2
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		animation.act(delta)
	}
	
	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (isTransform)
			applyTransform(batch, computeTransform())
		animation.draw(batch, parentAlpha)
		if (isTransform)
			resetTransform(batch)
	}
}
