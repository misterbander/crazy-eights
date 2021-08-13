package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.Group
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.SmoothInterpolator

open class SmoothMovable(x: Float = 0F, y: Float = 0F, rotation: Float = 0F) : Group()
{
	var xInterpolator = object : SmoothInterpolator(x)
	{
		override var value: Float
			get() = this@SmoothMovable.x
			set(value)
			{
				this@SmoothMovable.x = value
			}
	}
	var yInterpolator = object : SmoothInterpolator(y)
	{
		override var value: Float
			get() = this@SmoothMovable.y
			set(value)
			{
				this@SmoothMovable.y = value
			}
	}
	var rotationInterpolator = object : SmoothAngleInterpolator(rotation)
	{
		override var value: Float
			get() = this@SmoothMovable.rotation
			set(value)
			{
				this@SmoothMovable.rotation = value
			}
	}
	
	override fun act(delta: Float)
	{
		super.act(delta)
		xInterpolator.lerp(delta)
		yInterpolator.lerp(delta)
		rotationInterpolator.lerp(delta)
	}
	
	open fun setTargetPosition(x: Float, y: Float)
	{
		xInterpolator.target = x
		yInterpolator.target = y
	}
}
