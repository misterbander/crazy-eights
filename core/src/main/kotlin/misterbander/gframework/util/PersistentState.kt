package misterbander.gframework.util

/**
 * Specifies that this class contains state that needs to be persisted.
 */
interface PersistentState
{
	fun readState(mapper: PersistentStateMapper)
	fun writeState(mapper: PersistentStateMapper)
}
