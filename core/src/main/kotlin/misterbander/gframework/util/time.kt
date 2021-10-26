package misterbander.gframework.util

/**
 * Formats a duration of seconds to mm:ss.
 *
 * If the duration is longer than 1 hour, then it is formatted to hh:mm:ss.
 *
 * If the duration is longer than 1000 hours, then an infinity symbol is displayed instead.
 * @param durationSeconds duration in seconds
 * @param delimiter delimiter string to separate the units, defaults to " : "
 * @return Duration string formatted to mm:ss or hh:mm:ss.
 */
fun formatDuration(durationSeconds: Long, delimiter: String = " : "): String
{
	val seconds = durationSeconds%60
	val minutes = durationSeconds/60%60
	val hours = durationSeconds/3600
	
	if (hours > 1000)
		return "\u221E"
	val secondsStr = if (seconds <= 9) "0$seconds" else seconds.toString()
	if (hours > 0)
	{
		val minutesStr = if (minutes <= 9) "0$minutes" else minutes.toString()
		return "$hours$delimiter$minutesStr$delimiter$secondsStr"
	}
	return "$minutes$delimiter$secondsStr"
}
