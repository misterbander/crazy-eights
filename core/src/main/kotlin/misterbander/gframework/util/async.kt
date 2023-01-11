package misterbander.gframework.util

import com.badlogic.gdx.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Launches a new coroutine using [context], that executes [block] every [intervalSeconds] seconds.
 */
fun CoroutineScope.interval(
	context: CoroutineContext = EmptyCoroutineContext,
	intervalSeconds: Float,
	block: suspend () -> Unit
): Job = launch(context) {
	var lastNanoTime = TimeUtils.nanoTime()
	val intervalNanos = intervalSeconds*1000000000
	var delta = 0F
	while (true)
	{
		ensureActive()
		val now = TimeUtils.nanoTime()
		delta += now - lastNanoTime
		lastNanoTime = now
		while (delta >= intervalNanos)
		{
			ensureActive()
			delta -= intervalNanos
			block()
		}
		delay(1)
	}
}
