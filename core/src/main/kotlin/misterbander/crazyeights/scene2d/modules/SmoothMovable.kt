package misterbander.crazyeights.scene2d.modules

import misterbander.crazyeights.CrazyEights
import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.SmoothInterpolator

class SmoothMovable(
	parent: GObject<CrazyEights>,
	x: Float = parent.x,
	y: Float = parent.y,
	rotation: Float = parent.rotation
) : GModule<CrazyEights>(parent)
{
	val xInterpolator = object : SmoothInterpolator(x)
	{
		override var value: Float
			get() = parent.x
			set(value)
			{
				parent.x = value
			}
	}
	val yInterpolator = object : SmoothInterpolator(y)
	{
		override var value: Float
			get() = parent.y
			set(value)
			{
				parent.y = value
			}
	}
	val scaleInterpolator = object : SmoothInterpolator(parent.scaleX, 5F)
	{
		override var value: Float
			get() = parent.scaleX
			set(value) = parent.setScale(value)
	}
	val rotationInterpolator = object : SmoothAngleInterpolator(rotation, 5F)
	{
		override var value: Float
			get() = parent.rotation
			set(value)
			{
				parent.rotation = value
			}
	}
	var x by xInterpolator
	var y by yInterpolator
	var rotation by rotationInterpolator
	var scale by scaleInterpolator
	
	override fun update(delta: Float)
	{
		xInterpolator.lerp(delta)
		yInterpolator.lerp(delta)
		scaleInterpolator.lerp(delta)
		rotationInterpolator.lerp(delta)
	}
	
	fun setPosition(x: Float, y: Float)
	{
		this.x = x
		this.y = y
	}
	
	fun overwritePosition(x: Float, y: Float)
	{
		xInterpolator.overwrite(x)
		yInterpolator.overwrite(y)
	}
}
