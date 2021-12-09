package misterbander.crazyeights.model

interface ServerLockable
{
	var lockHolder: String?
	
	val isLocked: Boolean
		get() = lockHolder != null
	val canLock: Boolean
		get() = !isLocked
}
