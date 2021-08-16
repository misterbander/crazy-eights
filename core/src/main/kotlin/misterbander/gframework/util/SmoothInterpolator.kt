package misterbander.gframework.util

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
	fun set(value: Float)
	{
		this.value = value
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
