package misterbander.gframework.util

/**
 * Specifies that this class contains persistent state which can be saved and loaded from disk.
 */
interface PersistentState
{
	fun readState(mapper: PersistentStateMapper)
	fun writeState(mapper: PersistentStateMapper)
}
