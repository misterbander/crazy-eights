package misterbander.crazyeights.model

interface ServerLockable
{
	var lockHolder: User?
	
	val isLocked: Boolean
		get() = lockHolder != null
	val canLock: Boolean
		get() = !isLocked
}
