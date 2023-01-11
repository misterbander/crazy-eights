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
) : GModule
{
	val xInterpolator = SmoothInterpolator(x, get = { parent.x }) { parent.x = it }
	var x by xInterpolator
	val yInterpolator = SmoothInterpolator(y, get = { parent.y }) { parent.y = it }
	var y by yInterpolator
	val rotationInterpolator = SmoothAngleInterpolator(rotation, 5F, { parent.rotation }) { parent.rotation = it }
	var rotation by rotationInterpolator
	val scaleInterpolator = SmoothInterpolator(parent.scaleX, get = { parent.scaleX }) { parent.setScale(it) }
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
	
	fun snapPosition(x: Float, y: Float)
	{
		xInterpolator.snap(x)
		yInterpolator.snap(y)
	}
}
