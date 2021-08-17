package misterbander.sandboxtabletop.model

interface ServerDraggable
{
	var lockHolder: User?
	
	val isLocked: Boolean
		get() = lockHolder != null
}
