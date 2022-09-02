package misterbander.gframework.util

import com.badlogic.gdx.math.MathUtils
import ktx.math.vec2

/**
 * Provides a [lerp] method that interprets a float property or field as an angle of a vector in degrees and smoothly
 * interpolates it to a target angle.
 * @param initialValue initial value for the float property or field
 * @param smoothingFactor the smoothing factor. Higher smoothing factor means slower lerping.
 * @param get getter to return the float property or field
 * @param set setter to set the float property or field
 */
open class SmoothAngleInterpolator(
	initialValue: Float,
	smoothingFactor: Float = 2.5F,
	get: () -> Float,
	set: (Float) -> Unit
) : SmoothInterpolator(initialValue, smoothingFactor, get, set)
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
