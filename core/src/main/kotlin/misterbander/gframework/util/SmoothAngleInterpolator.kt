package misterbander.gframework.util

import com.badlogic.gdx.math.MathUtils
import ktx.math.vec2

/**
 * Wraps an angle in degrees and provides a [lerp] method that can smoothly interpolate the encapsulated angle
 * towards a target value. The angle lerping direction is always towards the smaller angle between the current angle
 * and the target angle.
 * @param value initial value for the wrapped angle
 * @property smoothingFactor the smoothing factor. Higher smoothing factor means slower lerping.
 */
abstract class SmoothAngleInterpolator(value: Float, smoothingFactor: Float = 2.5F) : SmoothInterpolator(value, smoothingFactor)
{
	private val angleVec = vec2(x = 1F)
	private val targetAngleVec = vec2(x = 1F)
	
	override fun lerp(delta: Float)
	{
		angleVec.setAngleDeg(value)
		targetAngleVec.setAngleDeg(target)
		var rotationDelta = targetAngleVec.angleDeg(angleVec)
		if (rotationDelta > 180)
			rotationDelta -= 360
		if (!MathUtils.isZero(rotationDelta, 1F))
			value += rotationDelta/smoothingFactor*delta*60
		else
			value = target
		value = value.mod(360F)
	}
}
