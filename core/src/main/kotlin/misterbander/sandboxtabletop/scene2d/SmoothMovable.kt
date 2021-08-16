package misterbander.sandboxtabletop.scene2d

import misterbander.gframework.scene2d.GObject
import misterbander.gframework.scene2d.module.GModule
import misterbander.gframework.util.SmoothAngleInterpolator
import misterbander.gframework.util.SmoothInterpolator
import misterbander.sandboxtabletop.SandboxTabletop

class SmoothMovable(parent: GObject<SandboxTabletop>) : GModule<SandboxTabletop>(parent)
{
	var xInterpolator = object : SmoothInterpolator(parent.x)
	{
		override var value: Float
			get() = parent.x
			set(value)
			{
				parent.x = value
			}
	}
	var yInterpolator = object : SmoothInterpolator(parent.y)
	{
		override var value: Float
			get() = parent.y
			set(value)
			{
				parent.y = value
			}
	}
	var rotationInterpolator = object : SmoothAngleInterpolator(parent.rotation)
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
		rotationInterpolator.lerp(delta)
	}
	
	fun setTargetPosition(x: Float, y: Float)
	{
		xInterpolator.target = x
		yInterpolator.target = y
	}
}
