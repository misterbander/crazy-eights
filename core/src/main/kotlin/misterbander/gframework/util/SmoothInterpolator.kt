package misterbander.gframework.util

import kotlin.reflect.KProperty

/**
 * Wraps a float value and provides a [lerp] method that can smoothly interpolate the encapsulated float towards a
 * target value.
 * @param value initial value for the wrapped float
 * @property smoothingFactor the smoothing factor. Higher smoothing factor means slower lerping.
 */
@Suppress("LeakingThis")
abstract class SmoothInterpolator(value: Float, var smoothingFactor: Float = 2.5F)
{
	/**
	 * The float value. You can use a backing field, or set it to interpolate any float variable/field.
	 */
	abstract var value: Float
	/** Target value to lerp to. */
	var target = value
	
	init
	{
		this.value = value
	}
	
	/**
	 * Sets both the value and target without lerping.
	 * @param value the value to set to
	 */
	fun overwrite(value: Float)
	{
		this.value = value
		target = value
	}
	
	/**
	 * Property delegate to return the target value.
	 */
	operator fun getValue(from: Any?, property: KProperty<*>): Float = target
	
	/**
	 * Property delegate to set the target value.
	 */
	operator fun setValue(from: Any?, property: KProperty<*>, value: Float)
	{
		target = value
	}
	
	/**
	 * Lerps the wrapped float to the target value. Should be called every frame.
	 * @param delta the time in seconds since the last render
	 */
	open fun lerp(delta: Float)
	{
		value += (target - value)/smoothingFactor*delta*60
	}
}
