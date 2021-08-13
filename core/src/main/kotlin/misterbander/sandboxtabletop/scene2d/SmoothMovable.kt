package misterbander.sandboxtabletop.scene2d

import com.badlogic.gdx.scenes.scene2d.Group
import ktx.math.vec2
import kotlin.math.abs

open class SmoothMovable : Group()
{
	var targetX = 0F
	var targetY = 0F
	var targetRotation = 0F
	var smoothingFactor = 2.5F
	
	private val rotationVec = vec2(x = 1F)
	private val targetRotationVec = vec2(x = 1F)
	
	override fun act(delta: Float)
	{
		super.act(delta)
		setPosition(x + (targetX - x)/smoothingFactor*delta*60, y + (targetY - y)/smoothingFactor*delta*60)
		
		rotationVec.setAngleDeg(rotation)
		targetRotationVec.setAngleDeg(targetRotation)
		val rotationDelta = rotationVec.angleDeg(targetRotationVec)
		rotation = if (abs(targetRotation - rotation) > 1) rotation + rotationDelta/smoothingFactor*delta*60 else targetRotation
	}
	
	open fun setTargetPosition(x: Float, y: Float)
	{
		targetX = x
		targetY = y
	}
}
