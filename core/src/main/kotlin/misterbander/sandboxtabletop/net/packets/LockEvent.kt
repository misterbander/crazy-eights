package misterbander.sandboxtabletop.net.packets

data class LockEvent(val serverObjectId: Int = -1, val lockerUsername: String? = null)
