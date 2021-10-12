package misterbander.gframework.util

import com.badlogic.gdx.utils.IntIntMap
import ktx.collections.defaultLoadFactor
import ktx.collections.defaultMapSize
import ktx.collections.set

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
