package misterbander.gframework.util

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.RandomXS128
import com.badlogic.gdx.utils.IntIntMap
import com.badlogic.gdx.utils.ObjectIntMap
import ktx.collections.*

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
 * @return A randomly chosen key based on the key to weight mapping given.
 */
fun <T> ObjectIntMap<T>.weightedRandom(): T
{
	val items: GdxArray<T> = keys().toArray()
	val cumulativeWeightArray = IntArray(size)
	for (i in cumulativeWeightArray.indices)
		cumulativeWeightArray[i] = this[items[i], Int.MIN_VALUE] + if (i == 0) 0 else cumulativeWeightArray[i - 1]
	
	val random = MathUtils.random(1, cumulativeWeightArray.last())
	
	// Perform binary search to select the smallest index with cumulative weight greater than or
	// equal to the randomly chosen number
	var low = 0
	var high = cumulativeWeightArray.lastIndex
	while (low != high)
	{
		val mid = (low + high)/2
		if (cumulativeWeightArray[mid] == random)
			return items[mid]
		if (cumulativeWeightArray[mid] > random)
			high = mid
		else
			low = mid + 1
	}
	return items[low]
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
