package misterbander.crazyeights.model

interface ServerLockable : ServerObject
{
	var lockHolder: String?
	
	val isLocked: Boolean
		get() = lockHolder != null
	val canLock: Boolean
		get() = !isLocked
}
