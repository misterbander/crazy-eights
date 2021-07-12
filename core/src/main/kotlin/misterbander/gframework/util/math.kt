package misterbander.gframework.util

import ktx.math.vec2

val tempVec = vec2()

/**
 * @param x1 x-component of the first point
 * @param y1 y-component of the first point
 * @param x2 x-component of the second point
 * @param y2 y-component of the second point
 * @return The angle from the first point to the second point, in degrees, where 0 is to the right and increases
 * anti-clockwise. Angles are between 0 and 360.
 */
fun angle(x1: Int, y1: Int, x2: Int, y2: Int): Float = angle(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

/**
 * @param x1 x-component of the first point
 * @param y1 y-component of the first point
 * @param x2 x-component of the second point
 * @param y2 y-component of the second point
 * @return The angle from the first point to the second point, in degrees, where 0 is to the right and increases
 * anti-clockwise. Angles are between 0 and 360.
 */
fun angle(x1: Float, y1: Float, x2: Float, y2: Float): Float
{
	return tempVec.set(x2 - x1, y2 - y1).angleDeg()
}

/**
 * Treats a range as a cycle and "wraps" an int within the cycle.
 */
infix fun Int.cycle(range: IntRange): Int
{
	var i = this
	val size = range.last - range.first + 1
	while (i > range.last)
		i -= size
	while (i < range.first)
		i += size
	return i
}

infix fun Float.cycle(range: ClosedFloatingPointRange<Float>): Float
{
	var f = this
	val size = range.endInclusive - range.start
	while (f > range.endInclusive)
		f -= size
	while (f < range.start)
		f += size
	return f
}
