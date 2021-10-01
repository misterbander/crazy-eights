package misterbander.sandboxtabletop.scene2d.modules

import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.SmoothInterpolator
import misterbander.sandboxtabletop.SandboxTabletop

class SmoothMovable(
	parent: GObject<SandboxTabletop>,
	x: Float = parent.x,
	y: Float = parent.y,
	rotation: Float = parent.rotation
) : GModule<SandboxTabletop>(parent)
{
	var xInterpolator = object : SmoothInterpolator(x)
	{
		override var value: Float
			get() = parent.x
			set(value)
			{
				parent.x = value
			}
	}
	var yInterpolator = object : SmoothInterpolator(y)
	{
		override var value: Float
			get() = parent.y
			set(value)
			{
				parent.y = value
			}
	}
	var scaleInterpolator = object : SmoothInterpolator(parent.scaleX, 5F)
	{
		override var value: Float
			get() = parent.scaleX
			set(value) = parent.setScale(value)
	}
	var rotationInterpolator = object : SmoothAngleInterpolator(rotation, 5F)
	{
		override var value: Float
			get() = parent.rotation
			set(value)
			{
				parent.rotation = value
			}
	}
	
	override fun update(delta: Float)
	{
		xInterpolator.lerp(delta)
		yInterpolator.lerp(delta)
		scaleInterpolator.lerp(delta)
		rotationInterpolator.lerp(delta)
	}
	
	fun setTargetPosition(x: Float, y: Float)
	{
		xInterpolator.target = x
		yInterpolator.target = y
	}
	
	fun setPositionAndTargetPosition(x: Float, y: Float)
	{
		xInterpolator.set(x)
		yInterpolator.set(y)
	}
}
