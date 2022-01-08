package misterbander.gframework.util

import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.utils.IntIntMap
import com.badlogic.gdx.utils.IntMap
import ktx.collections.*

/**
 * @param keysToValues will be added to the map.
 * @param initialCapacity initial capacity of the map. Will be resized if necessary.
 * @param loadFactor decides under what load the map is resized.
 * @return a new [IntMap].
 */
fun <T> gdxIntMapOf(
	vararg keysToValues: Pair<Int, T>,
	initialCapacity: Int = defaultMapSize,
	loadFactor: Float = defaultLoadFactor
): IntMap<T>
{
	val map = IntMap<T>(initialCapacity, loadFactor)
	keysToValues.forEach { map[it.first] = it.second }
	return map
}

/**
 * @param keysToValues will be added to the map.
 * @param initialCapacity initial capacity of the map. Will be resized if necessary.
 * @param loadFactor decides under what load the map is resized.
 * @return a new [IntIntMap].
 */
fun gdxIntIntMapOf(
	vararg keysToValues: Pair<Int, Int>,
	initialCapacity: Int = defaultMapSize,
	loadFactor: Float = defaultLoadFactor
): IntIntMap
{
	val map = IntIntMap(initialCapacity, loadFactor)
	keysToValues.forEach { map[it.first] = it.second }
	return map
}

/**
 * Shuffles a [GdxArray] using a specific seed.
 * @param seed the seed for the random number generator
 */
fun <T> GdxArray<T>.shuffle(seed: Long)
{
	val random = RandomXS128(seed)
	val items = items
	for (i in size - 1 downTo 0)
	{
		val ii = random.nextInt(i + 1)
		val temp = items[i]
		items[i] = items[ii]
		items[ii] = temp
	}
}
