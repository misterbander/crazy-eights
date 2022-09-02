package misterbander.gframework.util

import kotlin.reflect.KProperty

/**
 * Provides a [lerp] method that can smoothly interpolate a float property or field towards a target value.
 * @param initialValue initial value for the float property or field
 * @param smoothingFactor the smoothing factor. Higher smoothing factor means slower lerping.
 * @param get getter to return the float property or field
 * @param set setter to set the float property or field
 */
open class SmoothInterpolator(
	initialValue: Float,
	var smoothingFactor: Float = 2.5F,
	private val get: () -> Float,
	private val set: (Float) -> Unit
)
{
	var value: Float
		get() = get()
		set(value) = set(value)
	/** Target value to lerp to. */
	var target = initialValue
	
	init
	{
		set(initialValue)
	}
	
	/**
	 * Directly snaps the float property or field to [value] without lerping.
	 */
	fun snap(value: Float)
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
	 * Lerps the float property or field to the target value. Should be called every frame.
	 * @param delta the time in seconds since the last render
	 */
	open fun lerp(delta: Float)
	{
		value += (target - value)/smoothingFactor*delta*60
	}
}
